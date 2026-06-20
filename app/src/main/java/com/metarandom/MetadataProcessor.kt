package com.metarandom

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object MetadataProcessor {

    fun process(context: Context, uri: Uri): File {
        val outputDir = File(context.cacheDir, "processed").also { it.mkdirs() }
        val outputFile = File(outputDir, FakeMetadata.randomImageFilename())

        // Read original orientation BEFORE re-encoding strips it
        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        // Decode bitmap — this discards ALL original metadata
        val raw = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IllegalStateException("Cannot decode image from $uri")

        // Correct visual rotation so the output displays upright without EXIF aid
        val bitmap = correctOrientation(raw, orientation)
        if (bitmap !== raw) raw.recycle()

        // Write clean JPEG — all original metadata is gone
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        bitmap.recycle()

        // Inject fake EXIF
        val exif = ExifInterface(outputFile.absolutePath)
        FakeMetadata.applyToExif(exif)
        exif.saveAttributes()

        return outputFile
    }

    private fun correctOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90    -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180   -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270   -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL   -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f);  matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(-90f); matrix.postScale(-1f, 1f) }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
