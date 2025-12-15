package com.hotelsaidarshan.guestdocscanner.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import java.io.File
import kotlin.math.roundToInt

object DocumentRegions {
    // Base coordinate system assumed by the hardcoded rectangles.
    private const val BASE_WIDTH = 1080
    private const val BASE_HEIGHT = 1440

    val AADHAAR_REGION: Rect = Rect(50, 50, 1030, 750)

    val HANDWRITTEN_FIELDS: Map<String, Rect> = mapOf(
        "Coming From" to Rect(50, 800, 500, 900),
        "Going To" to Rect(500, 800, 1030, 900),
        "Mobile Number" to Rect(50, 920, 300, 1000),
        "Vehicle Number" to Rect(300, 920, 650, 1000),
        "Room Number" to Rect(650, 920, 1030, 1000),
    )

    fun scaleRectToBitmap(rect: Rect, bitmapWidth: Int, bitmapHeight: Int): Rect {
        val sx = bitmapWidth.toFloat() / BASE_WIDTH
        val sy = bitmapHeight.toFloat() / BASE_HEIGHT

        val left = (rect.left * sx).roundToInt()
        val top = (rect.top * sy).roundToInt()
        val right = (rect.right * sx).roundToInt()
        val bottom = (rect.bottom * sy).roundToInt()

        return Rect(left, top, right, bottom)
    }
}

object ImageProcessing {
    fun decodeBitmap(file: File): Bitmap {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: error("Unable to decode image")

        val rotationDegrees = runCatching {
            val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
            when (exif.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL,
            )) {
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        }.getOrDefault(0)

        if (rotationDegrees == 0) return bitmap

        val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        return rotated
    }

    fun crop(bitmap: Bitmap, rect: Rect): Bitmap {
        val safe = Rect(
            rect.left.coerceIn(0, bitmap.width - 1),
            rect.top.coerceIn(0, bitmap.height - 1),
            rect.right.coerceIn(1, bitmap.width),
            rect.bottom.coerceIn(1, bitmap.height),
        )

        val w = (safe.right - safe.left).coerceAtLeast(1)
        val h = (safe.bottom - safe.top).coerceAtLeast(1)

        return Bitmap.createBitmap(bitmap, safe.left, safe.top, w, h)
    }
}
