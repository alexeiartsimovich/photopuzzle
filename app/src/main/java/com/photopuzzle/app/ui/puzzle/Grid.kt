package com.photopuzzle.app.ui.puzzle

interface Grid {
    val rows: Int
    val columns: Int
    override fun toString(): String
}