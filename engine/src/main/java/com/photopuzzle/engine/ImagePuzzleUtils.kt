package com.photopuzzle.engine

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Size
import androidx.annotation.WorkerThread
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.absoluteValue
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

    fun getPosition(puzzle: ImagePuzzle, flattenPosition: Int): Position {
        return Position(
            row = flattenPosition / puzzle.rows,
            column = flattenPosition % puzzle.columns
        )
    }

    fun areSwappable(puzzle: ImagePuzzle, position1: Position, position2: Position): Boolean {
        if (!puzzle.getSquare(position1).isEmpty && !puzzle.getSquare(position2).isEmpty) {
            return false
        }
        if ((position1.row - position2.row).absoluteValue == 1
            && position1.column == position2.column) {
            return true
        }
        if (position1.row == position2.row
            && (position1.column - position2.column) == 1) {
            return true
        }
        return false
    }

    fun findEmptySquarePosition(puzzle: ImagePuzzle): Position? {
        for (i in 0 until puzzle.rows) {
            for (j in 0 until puzzle.columns) {
                if (puzzle.getSquare(i, j).isEmpty) {
                    return Position(i, j)
                }
            }
        }
        return null
    }

    fun findAdjacentEmptySquarePosition(puzzle: ImagePuzzle, row: Int, column: Int): Position? {
        for (i in -1..1) {
            for (j in -1..1) {
                if (i.absoluteValue == j.absoluteValue) {
                    continue
                }
                val otherRow = row + i
                val otherColumn = column + j
                getSquareOrNull(puzzle, otherRow, otherColumn)?.also { other ->
                    if (other.isEmpty) {
                        return Position(otherRow, otherColumn)
                    }
                }
            }
        }
        return null
    }

    private fun getSquareOrNull(puzzle: ImagePuzzle, row: Int, column: Int): ImagePuzzle.Square? {
        if (row in 0 until puzzle.rows && column in 0 until puzzle.columns) {
            return puzzle.getSquare(row, column)
        }
        return null
    }

    fun getSquareDimensionRatio(puzzle: ImagePuzzle): Float {
        if (puzzle.rows <= 0 || puzzle.columns <= 0) {
            return 1f
        }
        return puzzle.getSquare(0, 0).let { square ->
            if (square.image.intrinsicWidth > 0 && square.image.intrinsicHeight > 0) {
                square.image.intrinsicWidth.toFloat() / square.image.intrinsicHeight
            } else {
                1f
            }
        }
    }
}