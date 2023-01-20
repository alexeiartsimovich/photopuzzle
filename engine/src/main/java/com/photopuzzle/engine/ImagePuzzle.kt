package com.photopuzzle.engine

import android.graphics.drawable.Drawable
import android.util.Size

interface ImagePuzzle {
    val rows: Int
    val columns: Int
    fun getSquare(row: Int, column: Int): Square

    interface Square {
        val isEmpty: Boolean
        val image: Drawable
        val size: Size
    }
}