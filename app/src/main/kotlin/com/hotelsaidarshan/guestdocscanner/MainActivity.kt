package com.hotelsaidarshan.guestdocscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hotelsaidarshan.guestdocscanner.ui.screens.CameraScreen
import com.hotelsaidarshan.guestdocscanner.ui.screens.HomeScreen
import com.hotelsaidarshan.guestdocscanner.ui.screens.ProcessingScreen
import com.hotelsaidarshan.guestdocscanner.ui.screens.VerificationScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: GuestDocViewModel = viewModel()
            GuestDocScannerApp(vm)
        }
    }
}

@Composable
private fun GuestDocScannerApp(vm: GuestDocViewModel) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (vm.screen) {
                Screen.Home -> HomeScreen(onScanClick = vm::startNewScan)
                Screen.Camera -> CameraScreen(
                    capturedImageFile = vm.capturedImageFile,
                    createOutputFile = vm::createNewCaptureFile,
                    onImageCaptured = vm::onImageCaptured,
                    onRetake = vm::retake,
                    onProcess = vm::processCapturedImage,
                    onCancel = vm::cancelScan,
                )

                Screen.Processing -> ProcessingScreen()

                Screen.Verification -> VerificationScreen(
                    data = vm.guestData,
                    errorMessage = vm.errorMessage,
                    onDataChange = vm::updateData,
                    onDone = vm::done,
                )
            }
        }
    }
}
