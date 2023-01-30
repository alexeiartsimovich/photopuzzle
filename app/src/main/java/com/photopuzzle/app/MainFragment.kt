package com.photopuzzle.app

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import com.photopuzzle.app.di.DependencyProvider
import com.photopuzzle.engine.ImagePuzzleUi
import com.photopuzzle.engine.ImagePuzzleUtils
import com.photopuzzle.engine.ImagePuzzleView

class MainFragment : Fragment() {

    private val provider by lazy { DependencyProvider(requireContext().applicationContext as Application) }
    private var pickImageLauncher: ActivityResultLauncher<String>? = null

    private val gridSelectorView: GridSelectorView? get() = view?.findViewById(R.id.grid_selector_view)
    private val startButton: View? get() = view?.findViewById(R.id.start_button)
    private val puzzleView: ImagePuzzleView? get() = view?.findViewById(R.id.puzzle_view)

    private var selectedUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        pickImageLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent(),
            ActivityResultCallback { uri: Uri? ->
                this.selectedUri = uri
                if (uri != null) {
                    loadImagePuzzle(uri, gridSelectorView?.selectedGrid!!)
                }
            }
        )
        pickImage()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_main, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        gridSelectorView?.onGridSelectedCallback = GridSelectorView.OnGridSelectedCallback { _, grid ->
            selectedUri?.also { uri -> loadImagePuzzle(uri, grid) }
        }
        startButton?.setOnClickListener { startGame() }
        puzzleView?.apply {
            onPuzzleCompletedCallback = ImagePuzzleUi.OnPuzzleCompletedCallback { dispatchPuzzleCompleted() }
            isUiEnabled = false
        }
    }

    private fun pickImage() {
        val options = ActivityOptionsCompat.makeBasic()
        pickImageLauncher?.launch("image/*", options)
    }

    private fun loadImagePuzzle(uri: Uri, grid: Grid) {
        val context = this.context ?: return
        provider.backgroundExecutor.execute {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            val puzzle = ImagePuzzleUtils.createImagePuzzle(bitmap, grid.rows, grid.columns)
            provider.mainExecutor.execute {
                puzzleView?.loadImagePuzzle(puzzle)
            }
        }
    }

    private fun startGame() {
        gridSelectorView?.isEnabled = false
        startButton?.isEnabled = false
        puzzleView?.isUiEnabled = true
        puzzleView?.shuffleImagePuzzle()
    }

    private fun dispatchPuzzleCompleted() {
        Toast.makeText(requireContext(), "Puzzle completed!", Toast.LENGTH_LONG).show()
    }
}