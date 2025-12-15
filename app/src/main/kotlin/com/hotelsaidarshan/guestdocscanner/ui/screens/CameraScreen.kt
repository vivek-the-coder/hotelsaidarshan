package com.hotelsaidarshan.guestdocscanner.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import com.hotelsaidarshan.guestdocscanner.camera.CameraManager
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CameraScreen(
    capturedImageFile: File?,
    createOutputFile: (Context) -> File,
    onImageCaptured: (File) -> Unit,
    onRetake: () -> Unit,
    onProcess: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf<Boolean?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        showPermissionDialog = !granted
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasCameraPermission = granted
        if (!granted) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Camera permission required") },
            text = { Text("This app needs camera access to scan the document.") },
            confirmButton = {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Retry")
                }
            },
            dismissButton = {
                Button(onClick = onCancel) {
                    Text("Exit")
                }
            },
        )
    }

    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val cameraManager = remember { CameraManager(context, lifecycleOwner, previewView) }

    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission == true) {
            cameraManager.bindCameraUseCases {
                Toast.makeText(context, "Camera failed to start", Toast.LENGTH_LONG).show()
                onCancel()
            }
        }
    }

    val capturedBitmap by produceState<Bitmap?>(initialValue = null, key1 = capturedImageFile) {
        value = capturedImageFile?.let { file ->
            withContext(Dispatchers.IO) {
                com.hotelsaidarshan.guestdocscanner.utils.ImageProcessing.decodeBitmap(file)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (capturedBitmap != null) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Captured image",
                )
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { previewView },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onCancel) {
                Text("Cancel", fontSize = 16.sp)
            }

            if (capturedImageFile == null) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val outFile = createOutputFile(context)
                        cameraManager.takePhoto(outFile) { result ->
                            result
                                .onSuccess { onImageCaptured(it) }
                                .onFailure {
                                    Toast.makeText(context, "Capture failed", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                ) {
                    Text("Capture", fontSize = 18.sp)
                }
            } else {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onProcess()
                    },
                ) {
                    Text("Process", fontSize = 18.sp)
                }

                Button(onClick = onRetake) {
                    Text("Retake", fontSize = 16.sp)
                }
            }
        }
    }
}
