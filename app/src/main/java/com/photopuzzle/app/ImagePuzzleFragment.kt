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
    ): View? = inflater.inflate(R.layout.fragment_image_puzzle, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        gridSelectorView?.onGridSelectedCallback = GridSelectorView.OnGridSelectedCallback { _, grid ->
            selectedUri?.also { uri -> loadImagePuzzle(uri, grid) }
        }
        startButton?.setOnClickListener { startGame() }
        puzzleView?.apply {
            onPuzzleCompletedCallback = ImagePuzzleUi.OnPuzzleCompletedCallback { dispatchPuzzleCompleted() }
        }
        startNewGameButton?.apply {
            setOnClickListener { startNewGame() }
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
        beginDelayedTransition {
            Fade().apply {
                duration = 150L
            }
        }
        gridSelectorView?.apply {
            isEnabled = false
            visibility = View.INVISIBLE
        }
        startButton?.apply {
            isEnabled = false
            visibility = View.INVISIBLE
        }
        puzzleView?.shuffleImagePuzzle()
    }

    private fun dispatchPuzzleCompleted() {
        Toast.makeText(requireContext(), "Puzzle completed!", Toast.LENGTH_LONG).show()
        beginDelayedTransition {
            Fade().apply {
                duration = 150L
            }
        }
        startNewGameButton?.visibility = View.VISIBLE
    }

    private inline fun beginDelayedTransition(transition: () -> Transition) {
        (view as? ViewGroup)?.also { sceneRoot ->
            TransitionManager.beginDelayedTransition(sceneRoot, transition.invoke())
        }
    }

    private fun startNewGame() {
        (activity as? OnNewGameClickedCallback)?.onNewGameClicked()
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