package com.photopuzzle.engine

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import androidx.exifinterface.media.ExifInterface
import java.io.File


internal object BitmapUtils {
    private const val UNKNOWN_ORIENTATION: Int = ExifInterface.ORIENTATION_UNDEFINED

    @SuppressLint("Range")
    @WorkerThread
    fun getOrientation(context: Context, uri: Uri, filepath: String?): Int {
        // Using filepath
        if (!filepath.isNullOrBlank()) {
            val file = File(filepath)
            val exif = ExifInterface(file)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            if (orientation != UNKNOWN_ORIENTATION) {
                return orientation
            }
        }
        // Using image input stream
        kotlin.runCatching {
            val inputStream = context.contentResolver.openInputStream(uri)!!
            val exif = ExifInterface(inputStream)
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        }.onSuccess { orientation ->
            if (orientation != UNKNOWN_ORIENTATION) {
                return orientation
            }
        }
        // Using media store
        return kotlin.runCatching {
            val cursor: Cursor = context.contentResolver.query(
                uri, arrayOf(MediaStore.Images.ImageColumns.ORIENTATION), null, null, null)
                ?: return UNKNOWN_ORIENTATION
            cursor.use {
                if (it.moveToFirst()) {
                    it.getInt(it.getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION))
                } else {
                    throw NullPointerException()
                }
            }
        }.getOrDefault(UNKNOWN_ORIENTATION)
    }

    private fun getRotationInDegrees(orientation: Int): Float {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }

    @WorkerThread
    fun getBitmap(context: Context, uri: Uri, filepath: String?): Bitmap {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        val orientation = getOrientation(context, uri, filepath)
        val matrix = Matrix()
        matrix.preRotate(getRotationInDegrees(orientation))
        val adjustedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
            bitmap.width, bitmap.height, matrix, true)
        if (bitmap != adjustedBitmap) {
            bitmap.recycle()
        }
        return adjustedBitmap
    }
}