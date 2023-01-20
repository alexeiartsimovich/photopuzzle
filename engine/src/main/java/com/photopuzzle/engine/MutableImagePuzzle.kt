package com.photopuzzle.engine

import java.util.*
import kotlin.collections.ArrayList

internal class MutableImagePuzzle(
    imagePuzzle: ImagePuzzle
): ImagePuzzle by imagePuzzle {
    private val squareTable: List<MutableList<ImagePuzzle.Square>>

    init {
        squareTable = ArrayList<MutableList<ImagePuzzle.Square>>(rows)
        for (rowIndex in 0 until rows) {
            val row = ArrayList<ImagePuzzle.Square>(columns)
            for (columnIndex in 0 until columns) {
                row.add(imagePuzzle.getSquare(rowIndex, columnIndex))
            }
            squareTable.add(row)
        }
    }

    override fun getSquare(row: Int, column: Int): ImagePuzzle.Square {
        return squareTable[row][column]
    }

    fun swapSquares(fromRow: Int, fromColumn: Int, toRow: Int, toColumn: Int) {
        val tmp = squareTable[fromRow][fromColumn]
        squareTable[fromRow][fromColumn] = squareTable[toRow][toColumn]
        squareTable[toRow][toColumn] = tmp
    }
}