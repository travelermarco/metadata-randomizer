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

    /**
     * Processes [uri] by re-encoding it as a clean JPEG (stripping all original
     * metadata) and injecting fake EXIF. Returns the output file and a summary
     * string describing what was applied, for display in the confirmation screen.
     */
    fun process(context: Context, uri: Uri): Pair<File, String> {
        val filename  = FakeMetadata.randomImageFilename()
        val outputDir = File(context.cacheDir, "processed").also { it.mkdirs() }
        val outputFile = File(outputDir, filename)

        // Read all bytes in a single stream open — pipe-backed URIs are single-use
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Cannot open stream from $uri")

        // Read EXIF orientation from in-memory copy before re-encoding strips it
        val orientation = ExifInterface(bytes.inputStream()).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        // Decode bitmap — discards ALL original metadata
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("Cannot decode image from $uri")

        // Correct visual rotation so the output displays upright without EXIF aid
        val bitmap = correctOrientation(raw, orientation)
        if (bitmap !== raw) raw.recycle()

        // Write clean JPEG — all original metadata is gone at this point
        FileOutputStream(outputFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        bitmap.recycle()

        // Inject fake EXIF and get human-readable summary
        val exif = ExifInterface(outputFile.absolutePath)
        val summary = FakeMetadata.applyToExif(exif)
        exif.saveAttributes()

        return Pair(outputFile, "$filename · $summary")
    }

    private fun correctOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90    -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180   -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270   -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL   -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE    -> { matrix.postRotate(90f);  matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE   -> { matrix.postRotate(-90f); matrix.postScale(-1f, 1f) }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
