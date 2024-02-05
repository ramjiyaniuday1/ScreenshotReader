package com.alle.imagereader.ui.activity

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.annotation.SuppressLint
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
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
        data class ImageSelected(val image: Screenshot, val index: Int): Action()
        data class ShowCollectionSheet(val image: Screenshot): Action()
        object HideCollectionSheet: Action()
        data class TagCollection(val tag: String, val add: Boolean): Action()
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result: Boolean ->
        if (result) {
            Toast.makeText(this, "Permissions Granted..", Toast.LENGTH_SHORT).show()
            viewModel.setViewState(MainActivityViewModel.ViewState.PermissionGranted)
        } else {
            viewModel.setViewState(MainActivityViewModel.ViewState.NoPermission)
            Toast.makeText(
                this,
                "Permissions denied, Permissions are required to use the app..",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private val deleteResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.removeImageFromList()
        }
    }

    private val deletePendingImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.deleteScreenshot(this.contentResolver, null, null)
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions(this)
        setContent {
            ImageReaderTheme {
                MainContent(viewModel = viewModel, shareAction = { shareImage(it) },
                    deleteResultLauncher = deleteResultLauncher,
                    deletePendingImageLauncher =  deletePendingImageLauncher,
                    onBack = { finish() })
            }
        }
        setupObserver()
    }

    private fun setupObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadingState.collectLatest { viewState ->
                    (viewState as? MainActivityViewModel.ViewState.PermissionGranted)?.let {
                        val imageList = viewModel.getImages(this@MainActivity.contentResolver)
                        viewModel.setViewState(MainActivityViewModel.ViewState.Loaded(imageList))
                        if (imageList.isNotEmpty()) {
                            viewModel.setScreenshot(imageList.first(), 0)
                        }
                    }
                }
            }
        }
    }

    private fun requestPermissions(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(activity, READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(READ_MEDIA_IMAGES)
            }
            else {
                viewModel.setViewState(MainActivityViewModel.ViewState.PermissionGranted)
            }
        } else if (ActivityCompat.checkSelfPermission(activity, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(WRITE_EXTERNAL_STORAGE)
        } else if (ActivityCompat.checkSelfPermission(activity, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(READ_EXTERNAL_STORAGE);
        } else {
            //Toast.makeText(activity, "Permissions granted..", Toast.LENGTH_SHORT).show()
            viewModel.setViewState(MainActivityViewModel.ViewState.PermissionGranted)
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
fun MainContent(
    viewModel: MainActivityViewModel,
    shareAction: (screenshot: Screenshot) -> Unit,
    onBack: () -> Unit,
    deleteResultLauncher: ActivityResultLauncher<IntentSenderRequest>,
    deletePendingImageLauncher: ActivityResultLauncher<IntentSenderRequest>
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val context = LocalContext.current
    val lazyListState = rememberLazyListState()
    val scaffoldState = rememberBottomSheetScaffoldState()
    var showCollectionSheet by remember { mutableStateOf(false) }

    /* viewModel states */
    val loadingState = viewModel.loadingState.collectAsState()
    val showInfoView = viewModel.showInfoView.collectAsState()
    val selectedIndex = viewModel.selectedIndex.collectAsState()
    val selectedImage = viewModel.selectedScreenshot.collectAsState()

    val hideAllSheets:() -> Unit = {
        showCollectionSheet = false
        viewModel.hideInfoView()
    }

    /* todo should be moved to db */
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
                hideAllSheets()
                viewModel.deleteScreenshot(context.contentResolver, deleteResultLauncher, deletePendingImageLauncher).let {
                   /* if (it) {
                        viewModel.removeImageFromList()
                    }*/
                }
            }
            is MainActivity.Action.ImageSelected -> {
                viewModel.setScreenshot(action.image, action.index)
            }
            is MainActivity.Action.Info -> {
                viewModel.showInfoView(action.image)
            }
            is MainActivity.Action.Share -> {
                hideAllSheets()
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

    LaunchedEffect(key1 = showInfoView.value) {
        if (showInfoView.value is MainActivityViewModel.InfoViewState.Show) {
            scaffoldState.bottomSheetState.expand()
        } else {
            viewModel.hideInfoView()
        }
    }

    LaunchedEffect(scaffoldState.bottomSheetState.currentValue) {
        snapshotFlow { scaffoldState.bottomSheetState.currentValue }
            .mapLatest { it == SheetValue.Hidden  }
            .filter { it }
            .collectLatest {
                viewModel.hideInfoView()
            }
    }

    BackHandler {
        if (showCollectionSheet) {
            showCollectionSheet = false
        } else if (showInfoView.value is MainActivityViewModel.InfoViewState.Show) {
            viewModel.hideInfoView()
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White,
        bottomBar = {
            when(val data = loadingState.value) {
                is MainActivityViewModel.ViewState.Loaded -> {
                    BottomContent(
                        showInfoView = showInfoView, imageList = data.imageList, selectedImage = selectedImage,
                        action = action, selectedIndex= selectedIndex, screenWidth = screenWidth, lazyState = lazyListState)
                }
                is MainActivityViewModel.ViewState.Loading -> {
                    CircularProgressIndicator()
                }
                else -> {}
            }
        }
    ) {

        BottomSheetScaffold(modifier = Modifier.fillMaxSize(), sheetShadowElevation = 10.dp, scaffoldState = scaffoldState,
            sheetDragHandle = {}, contentColor = Color.Black , sheetSwipeEnabled = false, sheetContent = {
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
private fun BottomContent(
    showInfoView: State<MainActivityViewModel.InfoViewState>,
    imageList: List<Screenshot>,
    selectedImage: State<Screenshot?>,
    selectedIndex: State<Int>, screenWidth: Int,
    lazyState: LazyListState,
    action: (MainActivity.Action) -> Unit,
) {

    val infoOpened = remember {
        mutableStateOf(false)
    }
    Column {
        if (showInfoView.value is MainActivityViewModel.InfoViewState.Hide) {
            infoOpened.value = false
            ImageListRow(selectedIndex = selectedIndex, screenWidth = screenWidth, lazyState = lazyState,
                imageList = imageList) { image, index ->
                action(MainActivity.Action.ImageSelected(image, index))
            }
        } else {
            infoOpened.value = true
        }

        Divider()

        ActionMenuView(selectedImage, action, infoOpened)
    }
}

@Composable
fun ImageListRow(selectedIndex: State<Int>, imageList: List<Screenshot>,
                 screenWidth: Int, lazyState: LazyListState,
                 onClick:(img:Screenshot, index: Int) -> Unit) {

    LaunchedEffect(key1 = selectedIndex.value) {
        lazyState.animateScrollToItem(selectedIndex.value, -(screenWidth))
    }
    LazyRow(
        state = lazyState,
        modifier = Modifier.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(imageList) { index, item->
            val modifier = if (index != selectedIndex.value) {
                Modifier
                    .width(100.dp)
                    .height(100.dp)
            } else {
                Modifier
                    .width(120.dp)
                    .height(120.dp)
            }
            ImageCard(image = item, modifier = modifier, onClick = { onClick(it, index)})
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
            Modifier.fillMaxSize(),
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
