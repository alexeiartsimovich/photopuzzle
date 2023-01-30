package com.photopuzzle.engine

import android.graphics.drawable.Drawable
import android.util.Size

interface ImagePuzzle {
    val rows: Int
    val columns: Int
    fun getSquare(row: Int, column: Int): Square
    fun getSquare(position: Position): Square {
        return getSquare(position.row, position.column)
    }

    interface Square {
        val originalPosition: Position
        val isEmpty: Boolean
        val image: Drawable
        val size: Size
    }
}