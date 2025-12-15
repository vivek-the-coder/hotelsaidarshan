package com.hotelsaidarshan.guestdocscanner

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hotelsaidarshan.guestdocscanner.data.GuestDocumentData
import com.hotelsaidarshan.guestdocscanner.ocr.HandwritingOcrProcessor
import com.hotelsaidarshan.guestdocscanner.ocr.OcrProcessor
import com.hotelsaidarshan.guestdocscanner.utils.DocumentRegions
import com.hotelsaidarshan.guestdocscanner.utils.ImageProcessing
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GuestDocViewModel : ViewModel() {
    var screen: Screen by mutableStateOf(Screen.Home)
        private set

    var capturedImageFile: File? by mutableStateOf(null)
        private set

    var guestData: GuestDocumentData by mutableStateOf(GuestDocumentData.empty())
        private set

    var errorMessage: String? by mutableStateOf(null)
        private set

    private val printedOcr = OcrProcessor()
    private val handwritingOcr = HandwritingOcrProcessor()

    fun startNewScan() {
        clearSessionData(deleteCapturedImage = true)
        screen = Screen.Camera
    }

    fun cancelScan() {
        clearSessionData(deleteCapturedImage = true)
        screen = Screen.Home
    }

    fun onImageCaptured(file: File) {
        capturedImageFile?.delete()
        capturedImageFile = file
    }

    fun retake() {
        capturedImageFile?.delete()
        capturedImageFile = null
    }

    fun processCapturedImage() {
        val file = capturedImageFile ?: return
        screen = Screen.Processing
        errorMessage = null

        viewModelScope.launch {
            val result = runCatching { processFile(file) }
            if (result.isFailure) {
                Log.e(TAG, "OCR processing failed", result.exceptionOrNull())
                errorMessage = "OCR failed. Please type manually."
            }
            guestData = result.getOrNull() ?: GuestDocumentData.empty()
            capturedImageFile = null
            screen = Screen.Verification
        }
    }

    fun updateData(newData: GuestDocumentData) {
        guestData = newData
    }

    fun done() {
        clearSessionData(deleteCapturedImage = true)
        screen = Screen.Home
    }

    fun createNewCaptureFile(context: Context): File {
        val dir = context.cacheDir
        return File(dir, "guest_doc_${UUID.randomUUID()}.jpg")
    }

    private suspend fun processFile(file: File): GuestDocumentData {
        return withContext(Dispatchers.Default) {
            var fullBitmap: android.graphics.Bitmap? = null
            var aadhaarBitmap: android.graphics.Bitmap? = null
            val handwrittenBitmaps = mutableListOf<android.graphics.Bitmap>()

            try {
                fullBitmap = ImageProcessing.decodeBitmap(file)

                val scaledAadhaarRect = DocumentRegions.scaleRectToBitmap(
                    DocumentRegions.AADHAAR_REGION,
                    fullBitmap.width,
                    fullBitmap.height,
                )
                aadhaarBitmap = ImageProcessing.crop(fullBitmap, scaledAadhaarRect)

                val handwrittenCrops = DocumentRegions.HANDWRITTEN_FIELDS.mapValues { (_, rect) ->
                    val scaled = DocumentRegions.scaleRectToBitmap(rect, fullBitmap.width, fullBitmap.height)
                    ImageProcessing.crop(fullBitmap, scaled).also { handwrittenBitmaps += it }
                }

                val printedDeferred = async {
                    printedOcr.recognizeAadhaarFields(aadhaarBitmap)
                }

                val handwrittenDeferred = handwrittenCrops.mapValues { (_, bmp) ->
                    async { handwritingOcr.recognizeFromBitmap(bmp) }
                }

                val printed = printedDeferred.await()
                val handwritten = handwrittenDeferred.mapValues { it.value.await() }

                GuestDocumentData(
                    name = printed.name,
                    dob = printed.dob,
                    gender = printed.gender,
                    address = printed.address,
                    comingFrom = handwritten["Coming From"].orEmpty(),
                    goingTo = handwritten["Going To"].orEmpty(),
                    mobileNumber = handwritten["Mobile Number"].orEmpty().filter { it.isDigit() }.take(10),
                    vehicleNumber = handwritten["Vehicle Number"].orEmpty().uppercase().replace(" ", ""),
                    roomNumber = handwritten["Room Number"].orEmpty().replace(" ", ""),
                )
            } finally {
                handwrittenBitmaps.forEach { runCatching { it.recycle() } }
                runCatching { aadhaarBitmap?.recycle() }
                runCatching { fullBitmap?.recycle() }
                runCatching { file.delete() }
            }
        }
    }

    private fun clearSessionData(deleteCapturedImage: Boolean) {
        errorMessage = null
        guestData = GuestDocumentData.empty()
        if (deleteCapturedImage) {
            capturedImageFile?.delete()
        }
        capturedImageFile = null
    }

    override fun onCleared() {
        printedOcr.close()
        handwritingOcr.close()
        super.onCleared()
    }

    private companion object {
        private const val TAG = "GuestDocViewModel"
    }
}
