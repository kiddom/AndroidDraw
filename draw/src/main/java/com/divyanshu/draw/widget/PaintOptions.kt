package com.divyanshu.draw.widget

import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode

data class PaintOptions(val color: Int = Color.BLACK, val strokeWidth: Float = 8f, val alpha: Int = 255, val isEraserOn: Boolean = false) {
    private val paint: Paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    fun asPaint(): Paint {
        with(paint) {
            if (isEraserOn) {
                color = Color.TRANSPARENT
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            } else {
                color = this@PaintOptions.color
                xfermode = null
            }

            alpha = this@PaintOptions.alpha
            strokeWidth = this@PaintOptions.strokeWidth
        }

        return paint
    }
}