package com.divyanshu.draw.widget

import android.graphics.*

data class PaintOptions(val color: Int = Color.BLACK, val strokeWidth: Float = 8f, val alpha: Int = 255, val isEraserOn: Boolean = false, val text: String? = null, val textSize: Float? = null, val textStartPoint: PointF? = null) {
    private val paint: Paint = Paint().apply {
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    fun asPaint(): Paint {
        with(paint) {
            if (text == null) {
                if (isEraserOn) {
                    color = Color.TRANSPARENT
                    style = Paint.Style.STROKE
                    textSize = 1f
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                } else {
                    color = this@PaintOptions.color
                    style = Paint.Style.STROKE
                    textSize = 1f
                    xfermode = null
                }
            } else {
                color = this@PaintOptions.color
                style = Paint.Style.FILL
                textSize = this@PaintOptions.textSize!!
                xfermode = null
            }

            alpha = this@PaintOptions.alpha
            strokeWidth = this@PaintOptions.strokeWidth
        }

        return paint
    }
}
