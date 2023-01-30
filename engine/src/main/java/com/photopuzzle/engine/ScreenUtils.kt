package com.photopuzzle.engine

import android.content.Context
import android.util.TypedValue

fun Context.dp(value: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value, resources.displayMetrics)
}