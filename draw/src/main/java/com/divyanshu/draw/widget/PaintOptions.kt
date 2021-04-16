package com.divyanshu.draw.widget

import android.graphics.Color

data class PaintOptions(val color: Int = Color.BLACK, val strokeWidth: Float = 8f, val alpha: Int = 255, val isEraserOn: Boolean = false)