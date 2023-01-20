package com.photopuzzle.app

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import com.photopuzzle.app.di.DependencyProvider
import com.photopuzzle.engine.ImagePuzzle
import com.photopuzzle.engine.ImagePuzzleUtils
import com.photopuzzle.engine.ImagePuzzleView

class MainFragment : Fragment() {

    private val provider by lazy { DependencyProvider(requireContext().applicationContext as Application) }
    private var pickImageLauncher: ActivityResultLauncher<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent(),
            ActivityResultCallback { uri: Uri? ->
                uri?.also(::loadImagePuzzle)
            }
        )
        pickImage()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_main, container, false)

    private fun pickImage() {
        val options = ActivityOptionsCompat.makeBasic()
        pickImageLauncher?.launch("image/*", options)
    }

    private fun loadImagePuzzle(uri: Uri) {
        val context = this.context ?: return
        provider.backgroundExecutor.execute {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            val puzzle = ImagePuzzleUtils.createImagePuzzle(bitmap, 4, 4)
            provider.mainExecutor.execute {
                loadImagePuzzle(puzzle)
            }
        }
    }

    private fun loadImagePuzzle(puzzle: ImagePuzzle) {
        val puzzleView = view?.findViewById<ImagePuzzleView>(R.id.puzzle_view)
            ?: return
        puzzleView.loadImagePuzzle(puzzle)
    }
}