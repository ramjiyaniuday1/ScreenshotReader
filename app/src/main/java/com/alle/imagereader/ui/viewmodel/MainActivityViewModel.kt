package com.alle.imagereader.ui.viewmodel

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alle.imagereader.data.db.models.Screenshot
import com.alle.imagereader.domain.repo.ScreenshotRepo
import com.alle.imagereader.ui.utils.ImageUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Collections
import javax.inject.Inject


@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val screenshotRepo: ScreenshotRepo
): ViewModel() {

    private val TAG = "MainActivityViewModel"
    sealed class ViewState{
        object Loading: ViewState()
        object NoPermission: ViewState()
        object PermissionGranted: ViewState()
        data class Loaded(val imageList: MutableList<Screenshot>): ViewState()
    }

    sealed class InfoViewState {
        object Hide: InfoViewState()
        data class Show(val screenshot: Screenshot): InfoViewState()
    }

    private val _loadingState: MutableStateFlow<ViewState> = MutableStateFlow(ViewState.NoPermission)
    val loadingState by lazy { _loadingState.asStateFlow() }

    private val _showInfoView: MutableStateFlow<InfoViewState> = MutableStateFlow(InfoViewState.Hide)
    val showInfoView by lazy { _showInfoView.asStateFlow() }

    private val _selectedScreenshot: MutableStateFlow<Screenshot?> = MutableStateFlow(null)
    val selectedScreenshot by lazy { _selectedScreenshot.asStateFlow() }

    private var _selectedIndex = MutableStateFlow(0)
    val selectedIndex = _selectedIndex.asStateFlow()

    private val _permissionNeededForDelete = MutableStateFlow<IntentSender?>(null)
    val permissionNeededForDelete = _permissionNeededForDelete.asStateFlow()

    fun setViewState(viewState: ViewState) {
        _loadingState.value = viewState
    }

    fun setScreenshot(screenshot: Screenshot, index: Int) {
        viewModelScope.launch {
            screenshotRepo.findByUri(screenshot.fileUri)
                .flowOn(Dispatchers.IO).collectLatest {
                    _selectedIndex.value = index
                    if (it!=null) {
                        _selectedScreenshot.value = it
                    } else {
                        _selectedScreenshot.value = screenshot
                    }
                }
        }
    }

    fun saveNote(note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            selectedScreenshot.value?.copy(note = note)?.let {
                screenshotRepo.updateNote(it)
                _selectedScreenshot.value = it
            }
        }

    }

    fun showInfoView(image: Screenshot) {
        if (showInfoView.value is InfoViewState.Show) {
            toggleInfoViewState(image)
            return
        }
        viewModelScope.launch {
            screenshotRepo.findByUri(image.fileUri)
                .flowOn(Dispatchers.IO).collectLatest { screenshot ->
                    Log.d(TAG, "showInfoView: ${screenshot.toString()}")
                    if (screenshot == null) {
                        ImageUtils.convertCompressedByteArrayToBitmap(File(image.fileUri).readBytes())?.let {
                            val inputImage = InputImage.fromBitmap(it, 0)
                            runTextDetection(inputImage, image)
                        }
                    } else {
                        _selectedScreenshot.value = screenshot
                       toggleInfoViewState(screenshot)
                        Log.d(TAG, "showInfoView: $screenshot")
                    }
            }
        }

    }

    private fun toggleInfoViewState(screenshot: Screenshot) {
        if (_showInfoView.value is InfoViewState.Show) {
            _showInfoView.value = InfoViewState.Hide
        } else {
            _showInfoView.value = InfoViewState.Show(screenshot)
        }
    }

    fun hideInfoView() {
        _showInfoView.value = InfoViewState.Hide
    }

    fun getImages(contentResolver: ContentResolver): MutableList<Screenshot> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getImagesApi29(contentResolver)
        } else {
            getImagesLegacy()
        }
    }

    private fun getImagesLegacy(): MutableList<Screenshot> {
        val imgList: MutableList<Screenshot> = ArrayList()
        // val filePath = "/storage/emulated/0/DCIM/Camera"
        val rootPath: String = Environment.getExternalStorageDirectory().absolutePath
        val folderPath = "DCIM/Camera"

        val parentFile = File(rootPath + File.separator + folderPath)
        val files = parentFile.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.path.endsWith(".png") || file.path.endsWith(".jpg")) {
                    Screenshot(fileUri = file.path,
                        name = file.name, fileId = 0).apply {
                        imgList.add(this)
                    }
                }
            }
        }
        return imgList
    }

    private fun getImagesApi29(contentResolver: ContentResolver): MutableList<Screenshot> {

        val TAG = "IMAGES"
        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATA
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC"

        val imgList: MutableList<Screenshot> = ArrayList()

        contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.let { cursor ->
            val count: Int = cursor.getCount()
            Log.d(TAG, "getImagePath: $count")
            for (i in 0 until count-1) {

                cursor.moveToPosition(i)
                val dataColumnIndex: Int = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                val nameColumnIndex: Int = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val id: Int = cursor.getColumnIndex(MediaStore.Images.Media._ID)

                Screenshot(fileUri = cursor.getString(dataColumnIndex),
                    name = cursor.getString(nameColumnIndex), fileId = cursor.getLong(id)).apply {
                    imgList.add(this)
                }

            }
            cursor.close()
        }
        return imgList
    }

    fun deleteScreenshot(
        resolver: ContentResolver,
        deleteResultLauncher: ActivityResultLauncher<IntentSenderRequest>?,
        deletePendingImageLauncher: ActivityResultLauncher<IntentSenderRequest>?
    ) {

        selectedScreenshot.value?.let { screenshot ->

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val uriList: MutableList<Uri> = ArrayList()
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.getContentUri("external"),
                    screenshot.fileId
                )

                Collections.addAll(uriList, uri)
                val pendingIntent =
                    MediaStore.createDeleteRequest(resolver, uriList)
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    deleteResultLauncher?.launch(intentSenderRequest)

                } catch (e: Exception) {
                    Log.d("DELETE_IMAGE", "deleteScreenshot: $e")
                }
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                try {
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.getContentUri("external"),
                        screenshot.fileId
                    )
                    resolver.delete(
                        uri,
                        "${MediaStore.Images.Media._ID} = ?",
                        arrayOf(screenshot.fileId.toString())
                    )
                    removeImageFromList()
                } catch (securityException: SecurityException) {
                    val recoverableSecurityException =
                        securityException as? RecoverableSecurityException
                            ?: throw securityException

                    val intentSenderRequest =
                        IntentSenderRequest.Builder(recoverableSecurityException.userAction.actionIntent.intentSender)
                            .build()
                    deletePendingImageLauncher?.launch(intentSenderRequest)
                }
            } else {
                getImageDeleteUri(contentResolver = resolver, screenshot.fileUri)?.let {
                    resolver.delete(it, null, null)
                    removeImageFromList()
                }
            }
        }
    }

    private fun getImageDeleteUri(contentResolver: ContentResolver, path: String): Uri? {
        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            MediaStore.Images.Media.DATA + " = ?",
            arrayOf(path),
            null
        )
        val uri = if (cursor != null && cursor.moveToFirst())
            ContentUris.withAppendedId(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
            ) else null
        cursor?.close()
        return uri
    }

    private fun runTextDetection(inputImage: InputImage, image: Screenshot) {

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->

                val stringBuilder = StringBuilder("")
                val resultText = visionText.text
                Log.d(TAG, "runObjectDetection: $resultText")
                for (block in visionText.textBlocks) {
                    val blockText = block.text
                    val blockCornerPoints = block.cornerPoints
                    val blockFrame = block.boundingBox
                    for (line in block.lines) {
                        val lineText = line.text
                        val lineCornerPoints = line.cornerPoints
                        val lineFrame = line.boundingBox
                        for (element in line.elements) {
                            val elementText = element.text
                            val elementCornerPoints = element.cornerPoints
                            val elementFrame = element.boundingBox
                        }
                        stringBuilder.append(lineText)
                    }
                }

                viewModelScope.launch(Dispatchers.IO) {
                    val screenshot = Screenshot(fileUri = image.fileUri, name = image.name,
                        description = stringBuilder.toString(), fileId = image.fileId)
                    screenshotRepo.insert(screenshot)
                    _selectedScreenshot.value = screenshot
                    toggleInfoViewState(screenshot)
                }
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "runObjectDetection: $e")
            }

    }

    fun addTag(tag: String) {
        _selectedScreenshot.value?.collections?.toMutableList()?.let {
            it.add(tag)
            val collections = arrayListOf<String>().also {list ->
                list.addAll(it)
            }
            selectedScreenshot.value?.copy(collections = collections)?.let { screenshot ->
                viewModelScope.launch(Dispatchers.IO) {
                    screenshotRepo.updateCollection(screenshot = screenshot)
                    _selectedScreenshot.value = screenshot
                }
            }

        }
    }

    fun removeTag(tag: String) {
        _selectedScreenshot.value?.collections?.toMutableList()?.let {
            it.remove(tag)
            val collections = arrayListOf<String>().also {list ->
                list.addAll(it)
            }
            selectedScreenshot.value?.copy(collections = collections)?.let { screenshot ->
                viewModelScope.launch(Dispatchers.IO) {
                    screenshotRepo.updateCollection(screenshot = screenshot)
                    _selectedScreenshot.value = screenshot
                }
            }
        }
    }

    fun removeImageFromList() {
        (loadingState.value as? ViewState.Loaded)?.imageList?.toMutableList()?.let {
            it.remove(selectedScreenshot.value)
            _loadingState.update { state->
                ViewState.Loaded(it)
            }
            if (selectedIndex.value < it.size && selectedIndex.value > 0) {
                _selectedScreenshot.value = it[selectedIndex.value]
            }
        }
    }

}