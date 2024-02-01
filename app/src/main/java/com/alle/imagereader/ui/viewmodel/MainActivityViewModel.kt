package com.alle.imagereader.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alle.imagereader.data.db.models.Screenshot
import com.alle.imagereader.domain.models.ScreenshotRepo
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.io.File
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

    fun setViewState(viewState: ViewState) {
        _loadingState.value = viewState
    }

    fun setScreenshot(screenshot: Screenshot) {
        viewModelScope.launch {
            screenshotRepo.findByUri(screenshot.fileUri)
                .flowOn(Dispatchers.IO).collectLatest {
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
                .flowOn(Dispatchers.IO).collectLatest {screenshot ->
                    Log.d(TAG, "showInfoView: ${screenshot.toString()}")
                    if (screenshot == null) {
                        convertCompressedByteArrayToBitmap(File(image.fileUri).readBytes())?.let {
                            val inputImage = InputImage.fromBitmap(it, 0)
                            runObjectDetection(inputImage, image)
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


    private fun runObjectDetection(inputImage: InputImage, image: Screenshot) {

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

            }
        
       /* val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        labeler.process(image)
            .addOnSuccessListener { labels ->
                // Task completed successfully
                for (label in labels) {
                    val text = label.text
                    val confidence = label.confidence
                    val index = label.index
                    Log.d(TAG, "runObjectDetection: label: $text, confidence:$confidence, index: $index")
                }
            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                // ...
            }*/

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
        }
    }

    private fun convertCompressedByteArrayToBitmap(src: ByteArray): Bitmap? {
        return BitmapFactory.decodeByteArray(src, 0, src.size)
    }

}