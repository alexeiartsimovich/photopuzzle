package com.slidingpuzzle.engine

import kotlin.math.pow
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
        val squareCount = puzzle.rows * puzzle.columns
        val totalStepCount = random.nextInt(squareCount) + squareCount
        val initialState = SwapState(
            totalStepCount = totalStepCount,
            currentStep = 0,
            prevSide = null
        )
        moveNextRecursively(initialState)
    }

    private fun moveNextRecursively(state: SwapState) {
        if (state.stepsLeft <= 0) {
            isShuffling = false
            swapper.onFinishSwapping()
            return
        }
        swapStep(state) { side ->
            val nextState = state.copy(
                currentStep = state.currentStep + 1,
                prevSide = side
            )
            moveNextRecursively(nextState)
        }
    }

    private inline fun swapStep(state: SwapState, crossinline onFinished: (Side?) -> Unit) {
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

        if (state.prevSide != null && sides.size > 1) {
            // Don't return back to the previous side
            sides.remove(state.prevSide.reversed())
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
        val durationFactor = (state.stepsLeft.toFloat() / state.totalStepCount).pow(3)
        val duration = (durationFactor * 120)
            .toLong()
            .coerceIn(25L, 120L)
        swapper.onSwapSquares(
            fromPosition = emptySquarePosition,
            toPosition = targetPosition,
            duration = duration,
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

    private data class SwapState(
        val currentStep: Int,
        val totalStepCount: Int,
        val prevSide: Side?
    ) {
        val stepsLeft: Int get() = totalStepCount - currentStep
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