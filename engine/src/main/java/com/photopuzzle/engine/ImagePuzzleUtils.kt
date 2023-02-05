package com.photopuzzle.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Size
import androidx.annotation.WorkerThread
import kotlin.collections.ArrayList
import kotlin.math.absoluteValue

object ImagePuzzleUtils {
    @WorkerThread
    fun createImagePuzzle(
        context: Context,
        uri: Uri, filepath: String?,
        rows: Int, columns: Int
    ): ImagePuzzle {
        require(rows > 0)
        require(columns > 0)
        val rawBitmap = BitmapUtils.getBitmap(context, uri, filepath)
        val srcBitmap = cropBitmap(
            srcBitmap = rawBitmap,
            dimensionRatio = (columns.toFloat() / rows)
        )
        val squareTable = ArrayList<ArrayList<ImagePuzzle.Square>>(rows)
        val squareWidth = (srcBitmap.width.toFloat() / columns).toInt()
        val squareHeight = (srcBitmap.height.toFloat() / rows).toInt()
        for (rowIndex in 0 until rows) {
            val row = ArrayList<ImagePuzzle.Square>(columns)
            for (columnIndex in 0 until columns) {
                val x: Int = squareWidth * columnIndex
                val y: Int = squareHeight * rowIndex
                val image = Bitmap.createBitmap(srcBitmap, x, y, squareWidth, squareHeight)
                val isLast = rowIndex == rows - 1 && columnIndex == columns - 1
                val square = object : ImagePuzzle.Square {
                    override val id: Long = (rowIndex + columns + columnIndex).toLong()
                    override val originalPosition: Position = Position(rowIndex, columnIndex)
                    override val isStub: Boolean = isLast
                    override val image: Drawable = BitmapDrawable(image)
                    override val size: Size = Size(squareWidth, squareHeight)
                }
                row.add(square)
            }
            squareTable.add(row)
        }
        // We don't need these bitmaps anymore
        rawBitmap.runCatching { recycle() }
        srcBitmap.runCatching { recycle() }
        return object : ImagePuzzle {
            override val rows: Int = rows
            override val columns: Int = columns

            override fun getSquare(row: Int, column: Int): ImagePuzzle.Square {
                return squareTable[row][column]
            }
        }
    }

    @WorkerThread
    private fun cropBitmap(srcBitmap: Bitmap, dimensionRatio: Float): Bitmap {
        return if (srcBitmap.width / dimensionRatio >= srcBitmap.height) {
            val targetWidth = (srcBitmap.height * dimensionRatio).toInt()
            Bitmap.createBitmap(srcBitmap, srcBitmap.width / 2 - targetWidth / 2,
                0, targetWidth, srcBitmap.height)
        } else {
            val targetHeight = (srcBitmap.width / dimensionRatio).toInt()
            Bitmap.createBitmap(srcBitmap, 0, srcBitmap.height / 2 - targetHeight / 2,
                srcBitmap.width, targetHeight)
        }
    }

    fun getPosition(puzzle: ImagePuzzle, flattenPosition: Int): Position {
        return Position(
            row = flattenPosition / puzzle.rows,
            column = flattenPosition % puzzle.columns
        )
    }

    fun areSwappable(puzzle: ImagePuzzle, position1: Position, position2: Position): Boolean {
        if (!puzzle.getSquare(position1).isStub && !puzzle.getSquare(position2).isStub) {
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
                if (puzzle.getSquare(i, j).isStub) {
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
                    if (other.isStub) {
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

    fun isComplete(puzzle: ImagePuzzle): Boolean {
        for (i in 0 until puzzle.rows) {
            for (j in 0 until puzzle.columns) {
                val square = puzzle.getSquare(i, j)
                if (i != square.originalPosition.row || j != square.originalPosition.column) {
                    return false
                }
            }
        }
        return true
    }
}