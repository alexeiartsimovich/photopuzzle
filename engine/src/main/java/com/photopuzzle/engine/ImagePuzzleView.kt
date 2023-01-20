package com.photopuzzle.engine

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.absoluteValue

class ImagePuzzleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr), ImagePuzzleUi {

    private val recyclerView = RecyclerView(context).apply {
        isNestedScrollingEnabled = false
        layoutManager = GridLayoutManagerImpl(context)
    }

    init {
        val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        layoutParams.gravity = Gravity.CENTER
        addView(recyclerView, layoutParams)
    }

    override fun loadImagePuzzle(imagePuzzle: ImagePuzzle) {
        val adapter = SquareAdapter(
            puzzle = imagePuzzle,
            onClicked = { row, column ->
                moveSquare(row, column)
            },
            onDragStarted = { row, column ->

            }
        )
//        recyclerView.swapAdapter(adapter, false)
        (recyclerView.layoutManager as GridLayoutManagerImpl).apply {
            spanCount = imagePuzzle.columns
        }
        recyclerView.adapter = adapter
    }

    private fun getSquare(row: Int, column: Int): ImagePuzzle.Square? {
        val adapter = recyclerView.adapter as? SquareAdapter
            ?: return null
        val puzzle = adapter.puzzle
        if (row in 0 until puzzle.rows && column in 0 until puzzle.columns) {
            return puzzle.getSquare(row, column)
        }
        return null
    }

    private fun moveSquare(row: Int, column: Int): Boolean {
        for (i in -1..1) {
            for (j in -1..1) {
                if (i.absoluteValue == j.absoluteValue) {
                    continue
                }
                val otherRow = row + i
                val otherColumn = column + j
                getSquare(otherRow, otherColumn)?.also { other ->
                    if (other.isEmpty) {
                        swapSquaresAndNotify(row, column, otherRow, otherColumn)
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun swapSquaresAndNotify(fromRow: Int, fromColumn: Int, toRow: Int, toColumn: Int) {
        val adapter = recyclerView.adapter as? SquareAdapter
            ?: return
        adapter.swapSquares(fromRow, fromColumn, toRow, toColumn)
    }
}

private class SquareAdapter(
    puzzle: ImagePuzzle,
    private val onClicked: (row: Int, column: Int) -> Unit,
    private val onDragStarted: (row: Int, column: Int) -> Unit
) : RecyclerView.Adapter<SquareAdapter.ViewHolder>() {
    private val mutablePuzzle = MutableImagePuzzle(puzzle)
    val puzzle: ImagePuzzle get() = mutablePuzzle

    fun swapSquares(fromRow: Int, fromColumn: Int, toRow: Int, toColumn: Int) {
        mutablePuzzle.swapSquares(fromRow, fromColumn, toRow, toColumn)
        var fromPosition = fromRow * puzzle.columns + fromColumn
        var toPosition = toRow * puzzle.columns + toColumn
        if (fromPosition > toPosition) {
            val tmp = fromPosition
            fromPosition = toPosition
            toPosition = tmp
        }
        notifyItemMoved(fromPosition, toPosition)
        notifyItemMoved(toPosition - 1, fromPosition)
    }

    private fun getRowForPosition(position: Int): Int {
        return position / puzzle.rows
    }

    private fun getColumnForPosition(position: Int): Int {
        return position % puzzle.columns
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val imageView = AppCompatImageView(parent.context)
        imageView.setPadding(10, 10, 10, 10)
        return ViewHolder(imageView).apply {
            imageView.setOnClickListener {
                onClicked.invoke(
                    getRowForPosition(adapterPosition),
                    getColumnForPosition(adapterPosition)
                )
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val square = puzzle.getSquare(
            row = getRowForPosition(position),
            column = getColumnForPosition(position)
        )
        holder.bind(square)
    }

    override fun getItemCount(): Int {
        return puzzle.rows * puzzle.columns
    }

    class ViewHolder(private val imageView: ImageView): RecyclerView.ViewHolder(imageView) {
        fun bind(item: ImagePuzzle.Square) {
            imageView.setTag(R.id.id_image_square, item)
            if (item.isEmpty) {
                imageView.setImageDrawable(null)
            } else {
                imageView.setImageDrawable(item.image)
            }
        }
    }
}

private class GridLayoutManagerImpl(
    context: Context,
): GridLayoutManager(context, 1, GridLayoutManager.VERTICAL, false)