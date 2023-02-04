package com.photopuzzle.engine

import kotlin.random.Random

internal class ImagePuzzleShuffler(
    private val puzzle: ImagePuzzle,
    private val swapper: SquareSwapper
) {
    private val random: Random = Random.Default

    var isShuffling: Boolean = false
        private set

    fun shuffle() {
        if (isShuffling) {
            throw IllegalStateException("Cannot start shuffling because another shuffle is in progress")
        }
        isShuffling = true
        swapper.onStartSwapping()
        val steps = random.nextInt(10) + 20
        moveNextRecursively(0, steps, null)
    }

    private fun moveNextRecursively(step: Int, stepsLeft: Int, prevSide: Side?) {
        if (stepsLeft <= 0) {
            isShuffling = false
            swapper.onFinishSwapping()
            return
        }
        moveStep(prevSide) { nextSide ->
            moveNextRecursively(step + 1, stepsLeft - 1, nextSide)
        }
    }

    private fun moveStep(prevSide: Side?, onFinished: (Side?) -> Unit) {
        val emptySquarePosition = ImagePuzzleUtils.findEmptySquarePosition(puzzle)
        if (emptySquarePosition == null) {
            onFinished.invoke(null)
            return
        }

        val sides = ArrayList<Side>(4)
        if (emptySquarePosition.column > 0) {
            sides.add(Side.LEFT)
        }
        if (emptySquarePosition.row > 0) {
            sides.add(Side.TOP)
        }
        if (emptySquarePosition.column < puzzle.columns - 1) {
            sides.add(Side.RIGHT)
        }
        if (emptySquarePosition.row < puzzle.rows - 1) {
            sides.add(Side.BOTTOM)
        }

        if (prevSide != null && sides.size > 1) {
            // Don't return back to the previous side
            sides.remove(prevSide.reversed())
        }

        if (sides.isEmpty()) {
            onFinished.invoke(null)
            return
        }

        val side = sides[random.nextInt(sides.size)]
        val targetPosition = when (side) {
            Side.LEFT -> Position(emptySquarePosition.row, emptySquarePosition.column - 1)
            Side.TOP -> Position(emptySquarePosition.row - 1, emptySquarePosition.column)
            Side.RIGHT -> Position(emptySquarePosition.row, emptySquarePosition.column + 1)
            Side.BOTTOM -> Position(emptySquarePosition.row + 1, emptySquarePosition.column)
        }
        swapper.onSwapSquares(
            fromPosition = emptySquarePosition,
            toPosition = targetPosition,
            duration = 100L,
            onFinished = { onFinished.invoke(side) }
        )
    }

    private enum class Side {
        LEFT, TOP, RIGHT, BOTTOM;

        fun reversed(): Side = when(this) {
            LEFT -> RIGHT
            TOP -> BOTTOM
            RIGHT -> LEFT
            BOTTOM -> TOP
        }
    }

    interface SquareSwapper {
        fun onStartSwapping()
        fun onSwapSquares(
            fromPosition: Position,
            toPosition: Position,
            duration: Long,
            onFinished: () -> Unit
        )
        fun onFinishSwapping()
    }
}