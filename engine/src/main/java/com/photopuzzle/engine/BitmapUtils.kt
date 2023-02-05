package com.photopuzzle.engine

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.WorkerThread


internal object BitmapUtils {
    private const val UNKNOWN_ORIENTATION = -1

    @SuppressLint("Range")
    @WorkerThread
    fun getOrientation(context: Context, uri: Uri, filepath: String? = null): Int {
        val cursor: Cursor = context.contentResolver.query(
            uri, arrayOf(MediaStore.Images.ImageColumns.ORIENTATION), null, null, null)
            ?: return UNKNOWN_ORIENTATION
        return cursor.use {
            runCatching {
                if (it.moveToFirst()) {
                    it.getInt(it.getColumnIndex(MediaStore.Images.ImageColumns.ORIENTATION))
                } else {
                    throw NullPointerException()
                }
            }.getOrDefault(UNKNOWN_ORIENTATION)
        }
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
    fun getBitmap(context: Context, uri: Uri): Bitmap {
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        val orientation = getOrientation(context, uri)
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