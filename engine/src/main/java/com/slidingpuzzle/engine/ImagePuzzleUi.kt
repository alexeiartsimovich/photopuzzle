package com.slidingpuzzle.engine


interface ImagePuzzleUi {
    var onPuzzleCompletedCallback: OnPuzzleCompletedCallback?
    val isUiEnabled: Boolean
    var isNumbered: Boolean
    fun loadImagePuzzle(imagePuzzle: ImagePuzzle)
    fun shuffleImagePuzzle()

    fun interface OnPuzzleCompletedCallback {
        fun onPuzzleCompleted()
    }
}