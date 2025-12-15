package com.hotelsaidarshan.guestdocscanner.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hotelsaidarshan.guestdocscanner.utils.await

class OcrProcessor {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Runs ML Kit Text Recognition on the cropped Aadhaar region and extracts the required fields.
     * Aadhaar numbers are intentionally filtered out.
     */
    suspend fun recognizeAadhaarFields(bitmap: Bitmap): PrintedAadhaarData {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            val sanitized = sanitizeAadhaarText(result.text)
            extractPrintedFields(sanitized)
        } catch (t: Throwable) {
            Log.e(TAG, "Printed OCR failed", t)
            PrintedAadhaarData()
        }
    }

    fun close() {
        recognizer.close()
    }

    private fun sanitizeAadhaarText(text: String): String {
        return text
            .replace(Regex("\\b\\d{4}\\s?\\d{4}\\s?\\d{4}\\b"), "") // Aadhaar number
            .replace("\r", "")
            .trim()
    }

    private fun extractPrintedFields(text: String): PrintedAadhaarData {
        val lines = text
            .split("\n")
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.contains("Government", ignoreCase = true) }
            .filterNot { it.contains("Unique", ignoreCase = true) }
            .filterNot { it.contains("Identification", ignoreCase = true) }

        val dobLineIndex = lines.indexOfFirst { it.contains("DOB", ignoreCase = true) || it.contains("Birth", ignoreCase = true) || it.matches(Regex(".*\\b\\d{2}/\\d{2}/\\d{4}\\b.*")) }
        val dob = lines
            .firstNotNullOfOrNull { Regex("\\b\\d{2}/\\d{2}/\\d{4}\\b").find(it)?.value }
            ?: lines.firstNotNullOfOrNull { line ->
                if (line.contains("YOB", ignoreCase = true) || line.contains("Year", ignoreCase = true)) {
                    Regex("\\b(19|20)\\d{2}\\b").find(line)?.value
                } else null
            }.orEmpty()

        val genderLineIndex = lines.indexOfFirst { it.contains("MALE", true) || it.contains("FEMALE", true) }
        val gender = when {
            lines.any { it.contains("MALE", true) } -> "MALE"
            lines.any { it.contains("FEMALE", true) } -> "FEMALE"
            else -> ""
        }

        val nameIndex = lines.indexOfFirst { line ->
            line.length in 3..60 &&
                line.matches(Regex("[A-Za-z .]+")) &&
                !line.contains("DOB", true) &&
                !line.contains("Birth", true) &&
                !line.contains("MALE", true) &&
                !line.contains("FEMALE", true) &&
                !line.contains("Address", true)
        }
        val name = lines.getOrNull(nameIndex).orEmpty()

        val addressStartIndex = lines.indexOfFirst { it.contains("Address", ignoreCase = true) }
        val afterKeyInfoIndex = listOf(nameIndex, dobLineIndex, genderLineIndex)
            .filter { it >= 0 }
            .maxOrNull()
            ?: -1

        val addressLines = when {
            addressStartIndex >= 0 -> lines.drop(addressStartIndex + 1)
            afterKeyInfoIndex >= 0 -> lines.drop(afterKeyInfoIndex + 1)
            else -> emptyList()
        }

        val address = addressLines
            .joinToString(separator = ", ")
            .replace(Regex("\\s+"), " ")
            .trim()

        return PrintedAadhaarData(
            name = name,
            dob = dob,
            gender = gender,
            address = address,
            rawText = text,
        )
    }

    data class PrintedAadhaarData(
        val name: String = "",
        val dob: String = "",
        val gender: String = "",
        val address: String = "",
        val rawText: String = "",
    )

    private companion object {
        private const val TAG = "OcrProcessor"
    }
}
