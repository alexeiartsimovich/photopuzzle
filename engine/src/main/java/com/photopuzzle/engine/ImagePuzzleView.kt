package com.photopuzzle.engine

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class ImagePuzzleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr), ImagePuzzleUi {

    private val recyclerView = RecyclerView(context).apply {
        isNestedScrollingEnabled = false
        layoutManager = GridLayoutManagerImpl(context)
    }
    private var dragHelper: ItemTouchHelper? = null

    private var shuffler: ImagePuzzleShuffler? = null

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
            onDragStarted = { holder, row, column ->
                dragSquare(holder, row, column)
            }
        )
//        recyclerView.swapAdapter(adapter, false)
        (recyclerView.layoutManager as GridLayoutManagerImpl).apply {
            spanCount = imagePuzzle.columns
        }
        recyclerView.adapter = adapter
        val dragCallback = ItemMoveCallback(imagePuzzle)
        val dragHelper = ItemTouchHelper(dragCallback)
        dragHelper.attachToRecyclerView(recyclerView)
        this.dragHelper = dragHelper
        this.shuffler = ImagePuzzleShuffler(
            puzzle = adapter.puzzle,
            swapper = object : ImagePuzzleShuffler.SquareSwapper {
                override fun onStartSwapping() {
                    recyclerView.itemAnimator?.apply {
                        moveDuration = 80L
                    }
                }

                override fun onSwapSquares(
                    fromPosition: Position,
                    toPosition: Position,
                    duration: Long,
                    onFinished: () -> Unit
                ) {
                    adapter.swapSquares(
                        fromRow = fromPosition.row,
                        fromColumn = fromPosition.column,
                        toRow = toPosition.row,
                        toColumn = toPosition.column
                    )
                    recyclerView.post {
                        recyclerView.itemAnimator?.isRunning(onFinished)
                            ?: onFinished.invoke()
                    }
                }

                override fun onFinishSwapping() {
                    configureAnimations()
                }
            }
        )
        configureAnimations()
        recyclerView.post {
            shuffler?.shuffle()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (shuffler?.isShuffling == true) {
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    private fun configureAnimations() {
        recyclerView.itemAnimator?.apply {
            addDuration = 80L
            moveDuration = 120L
            changeDuration = 120L
            removeDuration = 80L
        }
    }

    private fun moveSquare(row: Int, column: Int): Boolean {
        val adapter = recyclerView.adapter as? SquareAdapter
            ?: return false
        val puzzle = adapter.puzzle
        val emptySquarePosition = ImagePuzzleUtils.findAdjacentEmptySquarePosition(puzzle, row, column)
            ?: return false
        swapSquaresAndNotify(row, column, emptySquarePosition.row, emptySquarePosition.column)
        return true
    }

    private fun dragSquare(holder: RecyclerView.ViewHolder, row: Int, column: Int): Boolean {
        val adapter = recyclerView.adapter as? SquareAdapter
            ?: return false
        val puzzle = adapter.puzzle
        val emptySquarePosition = ImagePuzzleUtils.findAdjacentEmptySquarePosition(puzzle, row, column)
            ?: return false
        val dragHelper = this.dragHelper
            ?: return false
        dragHelper.startDrag(holder)
        return true
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
    private val onDragStarted: (holder: RecyclerView.ViewHolder, row: Int, column: Int) -> Unit
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
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_square, parent, false)
        itemView.findViewById<View>(R.id.square_image).updateLayoutParams<ConstraintLayout.LayoutParams> {
            dimensionRatio = ImagePuzzleUtils.getSquareDimensionRatio(puzzle).toString()
            setMargins(itemView.context.dp(0.5f).toInt().coerceAtLeast(1))
        }

        return ViewHolder(itemView).apply {
            itemView.setOnClickListener {
                onClicked.invoke(
                    getRowForPosition(adapterPosition),
                    getColumnForPosition(adapterPosition)
                )
            }
//            imageView.setOnLongClickListener {
//                onDragStarted.invoke(
//                    this,
//                    getRowForPosition(adapterPosition),
//                    getColumnForPosition(adapterPosition)
//                )
//                true
//            }
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

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val squareImageView: ImageView = itemView.findViewById(R.id.square_image)

        fun bind(item: ImagePuzzle.Square) {
            itemView.setTag(R.id.id_image_square, item)
            if (item.isEmpty) {
                squareImageView.setImageDrawable(null)
            } else {
                squareImageView.setImageDrawable(item.image)
            }
        }
    }
}

private class GridLayoutManagerImpl(
    context: Context,
): GridLayoutManager(context, 1, GridLayoutManager.VERTICAL, false)

private class ItemMoveCallback(
    private val puzzle: ImagePuzzle
) : ItemTouchHelper.Callback() {
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val directions = ItemTouchHelper.LEFT or
                ItemTouchHelper.UP or
                ItemTouchHelper.RIGHT or
                ItemTouchHelper.DOWN
        return ItemTouchHelper.Callback.makeFlag(ItemTouchHelper.ACTION_STATE_DRAG, directions)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val position1 = ImagePuzzleUtils.getPosition(puzzle, viewHolder.adapterPosition)
        val position2 = ImagePuzzleUtils.getPosition(puzzle, target.adapterPosition)
        return ImagePuzzleUtils.areSwappable(puzzle, position1, position2)
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        throw IllegalStateException("Swipes not allowed!")
    }
}

private fun Context.dp(value: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value, resources.displayMetrics)
}