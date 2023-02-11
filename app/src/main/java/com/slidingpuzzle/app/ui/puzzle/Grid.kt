package com.slidingpuzzle.app.ui.puzzle

interface Grid {
    val rows: Int
    val columns: Int
    override fun toString(): String
}