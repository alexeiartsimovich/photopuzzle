package com.photopuzzle.app

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.view.forEach
import androidx.core.view.setPadding
import com.photopuzzle.engine.dp

class GridSelectorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val spinner = AppCompatSpinner(context)

    var onGridSelectedCallback: OnGridSelectedCallback? = null

    val selectedGrid: Grid? get() {
        return spinner.selectedItem as? Grid
    }

    init {
        val adapter = object : ArrayAdapter<Grid>(
            context,
            android.R.layout.simple_spinner_item
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return super.getView(position, convertView, parent).apply {
                    setPadding(parent.context.dp(8F).toInt())
                }
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                return super.getDropDownView(position, convertView, parent).apply {
                    setPadding(parent.context.dp(8F).toInt())
                }
            }
        }
        adapter.addAll(createGridItems())
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                parent?.adapter?.getItem(position)?.also { item ->
                    if (item is Grid) onGridSelectedCallback?.onGridSelected(this@GridSelectorView, item)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        addView(spinner)
    }

    private fun createGridItems(): List<Grid> {
        val items = ArrayList<Grid>(5)
        items.add(SimpleGrid(3, 3))
        items.add(SimpleGrid(4, 4))
        items.add(SimpleGrid(5, 5))
        items.add(SimpleGrid(6, 6))
        return items
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        forEach { child -> child.isEnabled = enabled }
    }

    fun interface OnGridSelectedCallback {
        fun onGridSelected(view: GridSelectorView, grid: Grid)
    }

    private class SimpleGrid(
        override val rows: Int,
        override val columns: Int,
        private val description: String = "$rows X $columns"
    ): Grid {
        override fun toString(): String = description
    }
}