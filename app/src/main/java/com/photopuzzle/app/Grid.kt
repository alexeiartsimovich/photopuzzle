package com.photopuzzle.app

interface Grid {
    val rows: Int
    val columns: Int
    override fun toString(): String
}