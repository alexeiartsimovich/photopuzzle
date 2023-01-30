package com.photopuzzle.engine


interface ImagePuzzleUi {
    var onPuzzleCompletedCallback: OnPuzzleCompletedCallback?
    var isUiEnabled: Boolean
    fun loadImagePuzzle(imagePuzzle: ImagePuzzle)
    fun shuffleImagePuzzle()

    fun interface OnPuzzleCompletedCallback {
        fun onPuzzleCompleted()
    }
}