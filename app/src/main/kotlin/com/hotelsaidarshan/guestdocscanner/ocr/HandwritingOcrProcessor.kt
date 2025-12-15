package com.hotelsaidarshan.guestdocscanner.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hotelsaidarshan.guestdocscanner.utils.await

class HandwritingOcrProcessor {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * For handwritten fields on paper, ML Kit Text Recognition usually works better than Digital Ink.
     * Digital Ink is included as an optional fallback for *on-screen ink* (if used later).
     */
    suspend fun recognizeFromBitmap(bitmap: Bitmap): String {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = textRecognizer.process(image).await()
            result.text
                .replace("\n", " ")
                .replace(Regex("\\s+"), " ")
                .trim()
        } catch (t: Throwable) {
            Log.e(TAG, "Handwriting OCR (bitmap) failed", t)
            ""
        }
    }

    /**
     * Recognizes an Ink object (strokes drawn on screen). This does not download models automatically
     * to keep the app usable without internet.
     */
    suspend fun recognizeFromInk(ink: Ink, languageTag: String = "en-US"): String {
        return try {
            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
                ?: return ""
            val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()

            val remoteModelManager = RemoteModelManager.getInstance()
            val isDownloaded = remoteModelManager.isModelDownloaded(model).await()
            if (!isDownloaded) {
                return ""
            }

            val recognizer = DigitalInkRecognition.getClient(
                DigitalInkRecognizerOptions.builder(model).build(),
            )

            val result = recognizer.recognize(ink).await()
            result.candidates.firstOrNull()?.text.orEmpty()
        } catch (t: Throwable) {
            Log.e(TAG, "Handwriting OCR (ink) failed", t)
            ""
        }
    }

    /** Optional: model download (not used by default). */
    suspend fun downloadInkModel(languageTag: String = "en-US"): Boolean {
        return try {
            val modelIdentifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
                ?: return false
            val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
            val remoteModelManager = RemoteModelManager.getInstance()
            remoteModelManager.download(model, DownloadConditions.Builder().build()).await()
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Ink model download failed", t)
            false
        }
    }

    fun close() {
        textRecognizer.close()
    }

    private companion object {
        private const val TAG = "HandwritingOcrProcessor"
    }
}
