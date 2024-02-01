package com.alle.imagereader.ui.activity

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.rememberImagePainter
import com.alle.imagereader.data.db.models.Collection
import com.alle.imagereader.data.db.models.Screenshot
import com.alle.imagereader.ui.theme.ImageReaderTheme
import com.alle.imagereader.ui.viewmodel.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import java.io.File
import java.lang.Exception
import java.util.Collections


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainActivityViewModel>()

    sealed class Action {
        data class Share(val screenshot: Screenshot): Action()
        data class Info(val image: Screenshot): Action()
        data class Delete(val screenshot: Screenshot): Action()
        object HideBottomSheet: Action()
        data class SaveNote(val note: String): Action()
        data class ImageSelected(val image: Screenshot): Action()
        data class ShowCollectionSheet(val image: Screenshot): Action()
        object HideCollectionSheet: Action()
        data class TagCollection(val tag: String, val add: Boolean): Action()
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions(this)
        setContent {
            ImageReaderTheme {
                MainContent(viewModel = viewModel, shareAction = { shareImage(it) })
            }
        }
        setupObserver()
    }

    private fun setupObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadingState.collectLatest {viewState->
                    (viewState as? MainActivityViewModel.ViewState.PermissionGranted)?.let{
                        val imageList = getImagePath(this@MainActivity)
                        viewModel.setViewState(MainActivityViewModel.ViewState.Loaded(imageList))
                        imageList.first()?.let {
                            viewModel.setScreenshot(it)
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            101 ->
                if (grantResults.isNotEmpty()) {
                    val permissionAccepted = grantResults[0] === PackageManager.PERMISSION_GRANTED
                    if (permissionAccepted) {
                        Toast.makeText(this, "Permissions Granted..", Toast.LENGTH_SHORT).show()
                        viewModel.setViewState(MainActivityViewModel.ViewState.PermissionGranted)
                        //getImagePath(this)
                    } else {
                        viewModel.setViewState(MainActivityViewModel.ViewState.NoPermission)
                        Toast.makeText(
                            this,
                            "Permissions denied, Permissions are required to use the app..",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    private fun checkPermission(ctx: Context): Boolean {
        // in this method we are checking if the permissions are granted or not and returning the result.
        val result = ContextCompat.checkSelfPermission(ctx, READ_MEDIA_IMAGES)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions(activity: Activity) {
        if (checkPermission(activity)) {
            Toast.makeText(activity, "Permissions granted..", Toast.LENGTH_SHORT).show()
            //getImagePath(ctx)
            viewModel.setViewState(MainActivityViewModel.ViewState.PermissionGranted)
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(READ_MEDIA_IMAGES, READ_EXTERNAL_STORAGE), 101
            )
        }
    }

    private fun shareImage(screenshot: Screenshot) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, Uri.parse(screenshot.fileUri))
            type = "image/*"
        }

        startActivity(Intent.createChooser(sendIntent, null))
    }

}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun MainContent(viewModel: MainActivityViewModel, shareAction:(screenshot: Screenshot) -> Unit) {

    val context = LocalContext.current
    val selectedImage = viewModel.selectedScreenshot.collectAsState()

    var showCollectionSheet by remember { mutableStateOf(false) }

    val collections = mutableListOf<Collection>().also {
        it.add(Collection(2,"Pet"))
        it.add(Collection(3,"Dog"))
        it.add(Collection(4,"Jeans"))
        it.add(Collection(5,"Books"))
        it.add(Collection(6,"Payment"))
        it.add(Collection(7,"Human"))
    }

    val action:(action: MainActivity.Action) -> Unit = {action ->
        when(action)  {
            is MainActivity.Action.Delete -> {
                deleteImage(context, action.screenshot).let {
                    viewModel.removeImageFromList()
                }
            }
            is MainActivity.Action.ImageSelected -> {
                /*viewModel.convertCompressedByteArrayToBitmap(File(action.image.fileUri).readBytes())?.let {
                    val inputImage = InputImage.fromBitmap(it, 0)
                    viewModel.runObjectDetection(inputImage, action.image)
                }*/
                viewModel.setScreenshot(action.image)
            }
            is MainActivity.Action.Info -> {
                viewModel.showInfoView(action.image)
            }
            is MainActivity.Action.Share -> {
                shareAction(action.screenshot)
            }

            is MainActivity.Action.SaveNote -> {
                viewModel.saveNote(action.note)
            }

            is MainActivity.Action.HideBottomSheet -> {
                viewModel.hideInfoView()
            }

            is MainActivity.Action.ShowCollectionSheet -> {
                showCollectionSheet = true
            }

            MainActivity.Action.HideCollectionSheet -> {
                showCollectionSheet = false
            }

            is MainActivity.Action.TagCollection -> {
                if (action.add) {
                    viewModel.addTag(action.tag)
                } else {
                    viewModel.removeTag(action.tag)
                }
            }
        }
    }
    val loadingState = viewModel.loadingState.collectAsState()
    val showInfoView = viewModel.showInfoView.collectAsState()
    val scaffoldState = rememberBottomSheetScaffoldState()

    val openBottomSheet = remember {
        derivedStateOf { showInfoView.value as? MainActivityViewModel.InfoViewState.Show }
    }.value

    LaunchedEffect(key1 = openBottomSheet) {
        openBottomSheet?.let {
            scaffoldState.bottomSheetState.expand()
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { scaffoldState.bottomSheetState.currentValue }
            .mapLatest { it == SheetValue.Hidden  }
            .filter { it }
            .collectLatest {
                viewModel.hideInfoView()
            }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = {
            when(val data = loadingState.value) {
                is MainActivityViewModel.ViewState.Loaded -> {
                    Content(
                        showInfoView = showInfoView, imageList = data.imageList, selectedImage = selectedImage,
                        action = action)
                }
                is MainActivityViewModel.ViewState.Loading -> {
                    CircularProgressIndicator()
                }
                else -> {}
            }
        }
    ) {

        BottomSheetScaffold(modifier = Modifier.fillMaxSize(), sheetShadowElevation = 10.dp, scaffoldState = scaffoldState,
            sheetDragHandle = {}, contentColor = Color.Black ,sheetContent = {
                if (showInfoView.value is MainActivityViewModel.InfoViewState.Show) {
                    InfoView(selectedImage, action)
                }
            }) {
            selectedImage.value?.let { it1 -> ImageCard(image = it1, modifier = Modifier.fillMaxSize(), onClick = {}) }
        }
        if (showCollectionSheet) {
            CollectionSheet(screenshot = selectedImage, collections = collections, action = action)
        }
    }
}

@Composable
private fun Content(
    showInfoView: State<MainActivityViewModel.InfoViewState>,
    imageList: List<Screenshot>,
    selectedImage: State<Screenshot?>,
    action:(MainActivity.Action) -> Unit) {
    Column {
        if (showInfoView.value is MainActivityViewModel.InfoViewState.Hide) {
            ImageListRow(imageList) {
                action(MainActivity.Action.ImageSelected(it))
            }
        }

        Divider()
        Row(modifier = Modifier
            .fillMaxWidth()
            .background(Color.White),
            horizontalArrangement = Arrangement.SpaceAround) {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    selectedImage.value?.let {
                        action(MainActivity.Action.Share(it))
                    }}.padding(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "", tint = Color.Gray)
                Text(text = "Share", color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    selectedImage.value?.let {  action(MainActivity.Action.Info(it)) }
                }.padding(10.dp))
            {
                Icon(imageVector = Icons.Default.Info, contentDescription = "", tint = Color.Gray)
                Text(text = "Info", color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable {
                    selectedImage.value?.let {  action(MainActivity.Action.Delete(it)) }
                }.padding(10.dp))
            {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "", tint = Color.Gray)
                Text(text = "Delete", color = Color.Gray)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun InfoView(screenshot: State<Screenshot?>, action:(MainActivity.Action) -> Unit) {
    var note by remember { mutableStateOf(screenshot.value?.note?:"") }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.White)
            .padding(16.dp),
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = note,
            textStyle = TextStyle(color = Color.Black),
            onValueChange = { note = it },
            keyboardActions = KeyboardActions(onDone = {
                action(MainActivity.Action.SaveNote(note))
                keyboardController?.hide()
            }), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Collections", fontWeight = FontWeight.Bold, color = Color.Black)
            Text(text = "Edit", fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.clickable {
                screenshot.value?.let {
                    action(MainActivity.Action.ShowCollectionSheet(it))
                }
            })
        }

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
        ) {
            screenshot.value?.collections?.forEach { collection ->
                Row(modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(color = Color(0xFFFFFFCC))
                    .padding(6.dp), horizontalArrangement = Arrangement.Center) {
                    Text(text = collection, color = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Description", fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = screenshot.value?.description?:"", color = Color.Black)

    }
}
@Composable
fun ImageListRow(imageList: List<Screenshot>,onClick:(img:Screenshot) -> Unit) {
    LazyRow(
        modifier = Modifier.padding(10.dp)
    ) {
        items(imageList.size) {
            ImageCard(image = imageList[it], modifier = Modifier
                .width(100.dp)
                .height(100.dp), onClick = onClick)
        }
    }
}

@Composable
fun ImageCard(image: Screenshot, modifier: Modifier, onClick:(img:Screenshot) -> Unit ) {
    Card(
        modifier = modifier
            .padding(3.dp)
            .clickable(onClick = {
                onClick(image)
            }),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .fillMaxHeight()
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val imgFile = File(image.fileUri)
            Image(
                painter = rememberImagePainter(data = imgFile),
                contentDescription = "image",
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CollectionSheet(screenshot: State<Screenshot?>, collections: List<Collection>, action: (MainActivity.Action) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        modifier = Modifier.fillMaxHeight(),
        contentColor = Color.Black,
        containerColor = Color.White,
        onDismissRequest = {
            action(MainActivity.Action.HideCollectionSheet)
        },
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeightIn(min = 25.dp)
                    .border(width = 2.dp, color = Color.Gray, RoundedCornerShape(10.dp))
                    .padding(8.dp)
            ) {
                screenshot.value?.collections?.forEach { collection ->
                    Row(modifier = Modifier
                        .clickable {
                            action(
                                MainActivity.Action.TagCollection(
                                    collection,
                                    false
                                )
                            )
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFFFFCC))
                        .padding(10.dp)) {
                        Text(text = collection, color = Color.Black)

                        Icon(imageVector = Icons.Default.Close, contentDescription = "close")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Select an option or create one", color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
            ) {
                collections.forEach { collection ->
                    if (screenshot.value?.collections?.contains(collection.name) == false) {
                        Row(modifier = Modifier
                            .clickable {
                                action(
                                    MainActivity.Action.TagCollection(
                                        collection.name,
                                        true
                                    )
                                )
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(color = Color(0xFFFFFFCC))
                            .padding(10.dp)) {
                            Text(text = collection.name, color = Color.Black)

                            Icon(imageVector = Icons.Default.Add, contentDescription = "add")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    }
}


private fun getImagePath(ctx: Context): MutableList<Screenshot> {

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
        MediaStore.Images.Media.DURATION,
        MediaStore.Images.Media.SIZE,
        MediaStore.Images.Media.DATA
    )

    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC"

    val imgList: MutableList<Screenshot> = ArrayList()

    ctx.applicationContext.contentResolver.query(
        collection,
        projection,
        null,
        null,
        sortOrder
    )?.let { cursor ->
        // below line is to get total number of images
        val count: Int = cursor.getCount()
        Log.d(TAG, "getImagePath: $count")
        // on below line we are running a loop to add
        // the image file path in our array list.
        for (i in 0 until count-1) {

            cursor.moveToPosition(i)
            // on below line we are getting image file path
            val dataColumnIndex: Int = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            val nameColumnIndex: Int = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val id: Long = cursor.getColumnIndex(MediaStore.Images.Media._ID).toLong()
            // after that we are getting the image file path
            // and adding that path in our array list.
            Screenshot(fileUri = cursor.getString(dataColumnIndex),
                name = cursor.getString(nameColumnIndex), fileId = id).apply {
                imgList.add(this)
            }

        }
        // after adding the data to our
        // array list we are closing our cursor.
        cursor.close()
    }
    return imgList
}

@Throws(SendIntentException::class)
private fun deleteImage(context: Context, screenshot: Screenshot): Boolean {
    val contentResolver: ContentResolver = context.contentResolver
    val uriList: MutableList<Uri> = ArrayList()
    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.getContentUri("external"), screenshot.fileId)

    Collections.addAll(uriList, uri)
    val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        MediaStore.createDeleteRequest(contentResolver, uriList)
    } else {
        TODO("VERSION.SDK_INT < R")
    }
    try {
        (context as Activity).startIntentSenderForResult(
            pendingIntent.intentSender,
            101, null, 0,
            0, 0, null
        )
        return true
    } catch (e: Exception) {
        return false
    }
    return false
}


/*
private fun runDetection(context: Context, url: String): InputImage? {
    val contentResolver = context.applicationContext.contentResolver
    val image: InputImage
    try {

      //  image = InputImage.fromByteArray(, 480, 360, 0, InputImage.IMAGE_FORMAT_YV12)
        return null
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return null
}

@Throws(IOException::class)
private fun getBitmapFromUri(url: String, contentResolver: ContentResolver): Bitmap? {
    val uri = Uri.parse(url)
    val parcelFileDescriptor: ParcelFileDescriptor? =
        contentResolver.openFileDescriptor(uri, "r")
    parcelFileDescriptor?.fileDescriptor ?.let {
        val image: Bitmap = BitmapFactory.decodeFileDescriptor(it)
        parcelFileDescriptor.close()
        return image
    }
    return null
}*/
