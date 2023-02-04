package com.photopuzzle.app

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.photopuzzle.app.di.DependencyProvider
import com.photopuzzle.engine.ImagePuzzleUi
import com.photopuzzle.engine.ImagePuzzleUtils
import com.photopuzzle.engine.ImagePuzzleView

class ImagePuzzleFragment : Fragment() {

    private val provider by lazy { DependencyProvider(requireContext().applicationContext as Application) }

    private val gridSelectorView: GridSelectorView? get() = view?.findViewById(R.id.grid_selector_view)
    private val startButton: View? get() = view?.findViewById(R.id.start_button)
    private val puzzleView: ImagePuzzleView? get() = view?.findViewById(R.id.puzzle_view)

    private var selectedUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        selectedUri = arguments?.getParcelable<Uri>(ARG_IMAGE_URI)
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

    private fun loadImagePuzzle(uri: Uri, grid: Grid) {
        val context = this.context ?: return
        provider.backgroundExecutor.execute {
            val puzzle = ImagePuzzleUtils.createImagePuzzle(context, uri, grid.rows, grid.columns)
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

    companion object {
        private const val ARG_IMAGE_URI = "image_uri"

        fun newInstance(imageUri: Uri): Fragment {
            return ImagePuzzleFragment().apply {
                arguments = bundleOf(ARG_IMAGE_URI to imageUri)
            }
        }
    }
}