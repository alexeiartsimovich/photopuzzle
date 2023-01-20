package com.photopuzzle.engine

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Size
import androidx.annotation.WorkerThread
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

object ImagePuzzleUtils {
    @WorkerThread
    fun createImagePuzzle(bitmap: Bitmap, rows: Int, columns: Int): ImagePuzzle {
        require(rows > 0)
        require(columns > 0)
        val squares = ArrayList<ImagePuzzle.Square>(rows * columns)
        val squareWidth = (bitmap.width.toFloat() / rows).roundToInt()
        val squareHeight = (bitmap.height.toFloat() / columns).roundToInt()
        for (i in 0 until rows) {
            for (j in 0 until columns) {
                val image = Bitmap.createBitmap(bitmap, squareWidth * j, squareHeight * i,
                    squareWidth, squareHeight)
                val isLast = i == rows - 1 && j == columns - 1
                val square = object : ImagePuzzle.Square {
                    override val isEmpty: Boolean = isLast
                    override val image: Drawable = BitmapDrawable(image)
                    override val size: Size = Size(image.width, image.height)
                }
                squares.add(square)
            }
        }
        return object : ImagePuzzle {
            override val rows: Int = rows
            override val columns: Int = columns

            override fun getSquare(row: Int, column: Int): ImagePuzzle.Square {
                return squares[this.rows * row + column]
            }
        }
    }
}