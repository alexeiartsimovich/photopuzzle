package com.slidingpuzzle.engine

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemAnimator

class ImagePuzzleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr), ImagePuzzleUi {

    private val recyclerView = RecyclerView(context).apply {
        overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        isNestedScrollingEnabled = false
        layoutManager = GridLayoutManagerImpl(context)
    }
    private val adapter: SquareAdapter? get() = recyclerView.adapter as? SquareAdapter
    private val itemAnimator: ItemAnimator get() = recyclerView.itemAnimator!!
    private var dragHelper: ItemTouchHelper? = null

    private var shuffler: ImagePuzzleShuffler? = null

    override var onPuzzleCompletedCallback: ImagePuzzleUi.OnPuzzleCompletedCallback? = null
    override var isUiEnabled: Boolean = false
        private set
    override var isNumbered: Boolean
        get() = adapter?.isNumbered ?: false
        set(value) {
            adapter?.isNumbered = value
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
                    // itemAnimator.moveDuration = 80L
                }

                override fun onSwapSquares(
                    fromPosition: Position,
                    toPosition: Position,
                    duration: Long,
                    onFinished: () -> Unit
                ) {
                    itemAnimator.moveDuration = duration
                    adapter.swapSquares(
                        fromRow = fromPosition.row,
                        fromColumn = fromPosition.column,
                        toRow = toPosition.row,
                        toColumn = toPosition.column
                    )
                    val delay: Long = itemAnimator.moveDuration
                    recyclerView.postDelayed(
                        {
                            if (itemAnimator.isRunning) {
                                itemAnimator.isRunning(onFinished)
                            } else {
                                onFinished.invoke()
                            }
                        }, delay)
                }

                override fun onFinishSwapping() {
                    configureAnimations()
                    checkIfPuzzleCompleted(notifyIfComplete = false)
                }
            }
        )
        configureAnimations()
        checkIfPuzzleCompleted(notifyIfComplete = false)
    }

    override fun shuffleImagePuzzle() {
        recyclerView.post {
            isUiEnabled = false
            adapter?.drawStubSquare = false
            shuffler?.shuffle()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (!isUiEnabled || shuffler?.isShuffling == true) {
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    private fun configureAnimations() {
        itemAnimator.apply {
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
        checkIfPuzzleCompleted(notifyIfComplete = true)
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

    @SuppressLint("NotifyDataSetChanged")
    private fun checkIfPuzzleCompleted(notifyIfComplete: Boolean) {
        val adapter = this.adapter ?: return
        val isComplete = ImagePuzzleUtils.isComplete(adapter.puzzle)
        isUiEnabled = !isComplete
        adapter.drawStubSquare = isComplete
        if (isComplete && notifyIfComplete) {
            onPuzzleCompletedCallback?.onPuzzleCompleted()
        }
    }
}

private class SquareAdapter(
    puzzle: ImagePuzzle,
    private val onClicked: (row: Int, column: Int) -> Unit,
    private val onDragStarted: (holder: RecyclerView.ViewHolder, row: Int, column: Int) -> Unit
) : RecyclerView.Adapter<SquareAdapter.ViewHolder>() {
    private val mutablePuzzle = MutableImagePuzzle(puzzle)
    val puzzle: ImagePuzzle get() = mutablePuzzle

    init {
        setHasStableIds(true)
    }

    var drawStubSquare: Boolean = false
        set(value) {
            field = value
            for (position in 0 until itemCount) {
                getSquareForPosition(position).let { square ->
                    if (square.isStub) {
                        notifyItemChanged(position)
                    }
                }
            }
        }

    var isNumbered: Boolean = false
        set(value) {
            if (field != value) {
                field = value
//                for (i in 0 until itemCount) {
//                    notifyItemChanged(i, PAYLOAD_NUMBERED)
//                }
                notifyDataSetChanged()
            }
        }

    fun swapSquares(fromRow: Int, fromColumn: Int, toRow: Int, toColumn: Int) {
        mutablePuzzle.swapSquares(fromRow, fromColumn, toRow, toColumn)
        var fromPosition = fromRow * puzzle.columns + fromColumn
        var toPosition = toRow * puzzle.columns + toColumn
        if (fromPosition > toPosition) {
            val tmp = fromPosition
            fromPosition = toPosition
            toPosition = tmp
        }
        if (fromPosition != toPosition) {
            notifyItemMoved(fromPosition, toPosition)
            if (toPosition - 1 != fromPosition) {
                notifyItemMoved(toPosition - 1, fromPosition)
            }
        }
    }

    private fun getRowForPosition(position: Int): Int {
        return position / puzzle.columns
    }

    private fun getColumnForPosition(position: Int): Int {
        return position % puzzle.columns
    }

    private fun getSquareForPosition(position: Int): ImagePuzzle.Square {
        return puzzle.getSquare(
            row = getRowForPosition(position),
            column = getColumnForPosition(position)
        )
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

    override fun getItemId(position: Int): Long = getSquareForPosition(position).id

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        private val squareImageView: ImageView = itemView.findViewById(R.id.square_image)
        private val numberTextView: TextView = itemView.findViewById(R.id.number_text_view)

        @SuppressLint("SetTextI18n")
        fun bind(item: ImagePuzzle.Square) {
            itemView.setTag(R.id.id_image_square, item)
            val shouldDraw = !item.isStub || drawStubSquare
            if (shouldDraw) {
                squareImageView.setImageDrawable(item.image)
            } else {
                squareImageView.setImageDrawable(null)
            }
            numberTextView.apply {
                isVisible = isNumbered && shouldDraw
                text = (item.originalIndex + 1).toString()
            }
        }
    }

    companion object {
        private val PAYLOAD_NUMBERED = Any()
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