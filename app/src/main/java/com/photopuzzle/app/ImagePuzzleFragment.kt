package com.photopuzzle.app

import android.app.Application
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.photopuzzle.app.di.DependencyProvider
import com.photopuzzle.engine.ImagePuzzleUi
import com.photopuzzle.engine.ImagePuzzleUtils
import com.photopuzzle.engine.ImagePuzzleView

class ImagePuzzleFragment : Fragment() {

    fun interface OnNewGameClickedCallback {
        fun onNewGameClicked()
    }

    private val provider by lazy { DependencyProvider(requireContext().applicationContext as Application) }

    private val gridSelectorView: GridSelectorView? get() = view?.findViewById(R.id.grid_selector_view)
    private val startButton: View? get() = view?.findViewById(R.id.start_button)
    private val puzzleView: ImagePuzzleView? get() = view?.findViewById(R.id.puzzle_view)
    private val startNewGameButton: View? get() = view?.findViewById(R.id.start_new_game_button)

    private var uri: Uri? = null
    private var filepath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        uri = arguments?.getParcelable<Uri>(ARG_IMAGE_URI)
        filepath = arguments?.getString(ARG_IMAGE_FILEPATH)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_image_puzzle, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        gridSelectorView?.onGridSelectedCallback = GridSelectorView.OnGridSelectedCallback { _, grid ->
            loadImagePuzzle(grid)
        }
        startButton?.setOnClickListener { startGame() }
        puzzleView?.apply {
            onPuzzleCompletedCallback = ImagePuzzleUi.OnPuzzleCompletedCallback { dispatchPuzzleCompleted() }
        }
        startNewGameButton?.apply {
            setOnClickListener { startNewGame() }
        }
    }

    private fun loadImagePuzzle(grid: Grid) {
        val context = this.context ?: return
        val uri: Uri = this.uri ?: return
        val filepath: String? = this.filepath
        provider.backgroundExecutor.execute {
            val puzzle = ImagePuzzleUtils.createImagePuzzle(context, uri, filepath, grid.rows, grid.columns)
            provider.mainExecutor.execute {
                puzzleView?.loadImagePuzzle(puzzle)
            }
        }
    }

    private fun startGame() {
        gridSelectorView?.apply {
            isEnabled = false
            animateVisibility(this, View.INVISIBLE)
        }
        startButton?.apply {
            isEnabled = false
            animateVisibility(this, View.INVISIBLE)
        }
        puzzleView?.shuffleImagePuzzle()
    }

    private fun dispatchPuzzleCompleted() {
        Toast.makeText(requireContext(), R.string.puzzle_completed, Toast.LENGTH_LONG).show()
        startNewGameButton?.apply {
            animateVisibility(this, View.VISIBLE)
        }
    }

    private fun startNewGame() {
        (activity as? OnNewGameClickedCallback)?.onNewGameClicked()
    }

    private fun animateVisibility(view: View, visibility: Int) {
        // Do NOT use the TransitionManager here, it breaks the ImagePuzzleView
        val alpha = if (visibility == View.VISIBLE) 1f else 0f
        if (visibility == View.VISIBLE) {
            view.alpha = 0f
            view.visibility = View.VISIBLE
        }
        view.animate()
            .alpha(alpha)
            .setDuration(200L)
            .start()
    }

    companion object {
        private const val ARG_IMAGE_URI = "image_uri"
        private const val ARG_IMAGE_FILEPATH = "image_filepath"

        fun newInstance(imageUri: Uri, filepath: String?): Fragment {
            return ImagePuzzleFragment().apply {
                arguments = bundleOf(
                    ARG_IMAGE_URI to imageUri,
                    ARG_IMAGE_FILEPATH to filepath
                )
            }
        }
    }
}