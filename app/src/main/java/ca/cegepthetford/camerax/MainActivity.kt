package ca.cegepthetford.camerax

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.ui.graphics.asImageBitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.impl.utils.MatrixExt.postRotate
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ca.cegepthetford.camerax.ui.theme.CameraXTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private fun possedePermissionsRequises() : Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("CoroutineCreationDuringComposition")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraXTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val porteeCoRoutine = rememberCoroutineScope()

                    if(!this.possedePermissionsRequises()) {
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(
                                android.Manifest.permission.CAMERA,
                                android.Manifest.permission.RECORD_AUDIO
                            ),
                            0
                        )
                    }

                    val controleurCamera = remember {
                        LifecycleCameraController(applicationContext).apply {
                            setEnabledUseCases(
                                CameraController.IMAGE_CAPTURE or
                                        CameraController.VIDEO_CAPTURE
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        ApercuCamera(controleur = controleurCamera)

                        var bitmaps = remember { mutableStateListOf<Bitmap>() }
                        val etatEchafaudage = rememberBottomSheetScaffoldState()
                        BottomSheetScaffold(
                            scaffoldState = etatEchafaudage,
                            sheetPeekHeight = 0.dp,
                            sheetContent = {
                                LazyVerticalStaggeredGrid(
                                    columns = StaggeredGridCells.Fixed(2),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalItemSpacing = 16.dp,
                                    contentPadding = PaddingValues(16.dp)
                                ) {
                                    items(bitmaps) {bitmap ->
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                        )
                                    }
                                }
                            }
                        ) {padding ->
                            // Englobe notre scaffold... Ne pas utiliser le padding...
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = { Text("Photographie") },
                                        colors = TopAppBarDefaults.topAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            titleContentColor = MaterialTheme.colorScheme.primary,
                                        ),
                                        actions = {
                                            IconButton(
                                                onClick = {
                                                    controleurCamera.cameraSelector =
                                                        if(controleurCamera.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                                                            CameraSelector.DEFAULT_FRONT_CAMERA
                                                        else
                                                            CameraSelector.DEFAULT_BACK_CAMERA
                                                },
                                                modifier = Modifier.offset(16.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Cameraswitch,
                                                    contentDescription = "Inverser camera"
                                                )
                                            }
                                        }
                                    )
                                },
                                bottomBar = {
                                    BottomAppBar(
                                        actions = {
                                            IconButton(onClick = {
                                                controleurCamera.takePicture(
                                                    ContextCompat.getMainExecutor(applicationContext),
                                                    object : ImageCapture.OnImageCapturedCallback() {
                                                        override fun onCaptureSuccess(image: ImageProxy) {
                                                            super.onCaptureSuccess(image)
                                                            // importation Android Graphics
                                                            val matrix = Matrix().apply {
                                                                postRotate(image.imageInfo.rotationDegrees.toFloat())
                                                            }
                                                            val imageRotation = Bitmap.createBitmap(
                                                                image.toBitmap(),0,0,
                                                                image.width,image.height,
                                                                matrix,true
                                                            )
                                                            bitmaps.add(imageRotation)
                                                        }
                                                        override fun onError(exception: ImageCaptureException) {
                                                            super.onError(exception)
                                                            Log.d(
                                                                "Camera",
                                                                "Ne peut prendre la photo",
                                                                exception
                                                            )
                                                        }
                                                    }
                                                )

                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoCamera,
                                                    contentDescription = "Prise photo"
                                                )
                                            }
                                            IconButton(onClick = {

                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Photo,
                                                    contentDescription = "Album photo"
                                                )
                                            }
                                            porteeCoRoutine.launch {
                                                etatEchafaudage.bottomSheetState.expand()
                                            }



                                        }
                                    )
                                }
                            ) { padding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(padding)
                                ) {
                                    ApercuCamera(controleur = controleurCamera)
                                }
                            }
                        }

                    }

                }
            }
        }
    }
}



@Composable
fun ApercuCamera(
    controleur : LifecycleCameraController
) {
    val proprietaireCycleVie = LocalLifecycleOwner.current
    AndroidView(
        factory = {
            PreviewView(it).apply {
                this.controller = controleur
                controleur.bindToLifecycle(proprietaireCycleVie)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
