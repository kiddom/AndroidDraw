package com.divyanshu.draw.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils
import java.util.*

class DrawView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var canvasColor = Color.WHITE

    private var paths = LinkedHashMap<MyPath, PaintOptions>()

    private var lastPaths = LinkedHashMap<MyPath, PaintOptions>()
    private var undonePaths = LinkedHashMap<MyPath, PaintOptions>()

    private var path = MyPath()
    private var paintOptions = PaintOptions()

    private var curX = 0f
    private var curY = 0f
    private var startX = 0f
    private var startY = 0f
    private var isSaving = false
    private var isStrokeWidthBarEnabled = false

    var isEraserOn: Boolean
        get() = paintOptions.isEraserOn
        set(value) {
            paintOptions = paintOptions.copy(isEraserOn = value)
            invalidate()
        }

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun addText(text: String, textSize: Float, x: Float, y: Float) {
        path.reset()

        val textStartPoint = PointF(x, y)
        paintOptions = paintOptions.copy(text = text, textSize = textSize, textStartPoint = textStartPoint)
        paths[path] = paintOptions
        path = MyPath()

        invalidate()
    }

    fun undo() {
        if (paths.isEmpty() && lastPaths.isNotEmpty()) {
            paths = lastPaths.clone() as LinkedHashMap<MyPath, PaintOptions>
            lastPaths.clear()
            invalidate()

            return
        }

        if (paths.isEmpty()) {
            return
        }

        val lastPath = paths.values.lastOrNull()
        val lastKey = paths.keys.lastOrNull()

        paths.remove(lastKey)

        if (lastPath != null && lastKey != null) {
            undonePaths[lastKey] = lastPath
        }

        invalidate()
    }

    fun redo() {
        if (undonePaths.keys.isEmpty()) {
            return
        }

        val lastKey = undonePaths.keys.last()
        addPath(lastKey, undonePaths.values.last())
        undonePaths.remove(lastKey)
        invalidate()
    }

    fun setColor(newColor: Int) {
        @ColorInt
        val alphaColor = ColorUtils.setAlphaComponent(newColor, paintOptions.alpha)
        paintOptions = paintOptions.copy(color = alphaColor)

        if (isStrokeWidthBarEnabled) {
            invalidate()
        }
    }

    fun setAlpha(alpha: Int) {
        paintOptions = paintOptions.copy(alpha = alpha)
        setColor(paintOptions.color)
    }

    fun setStrokeWidth(strokeWidth: Float) {
        paintOptions = paintOptions.copy(strokeWidth = strokeWidth)

        if (isStrokeWidthBarEnabled) {
            invalidate()
        }
    }

    fun getBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(canvasColor)
        isSaving = true
        draw(canvas)
        isSaving = false

        return bitmap
    }

    fun addPath(path: MyPath, options: PaintOptions) {
        paths[path] = options
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for ((myPath, paintOptions) in paths) {
            val paint = paintOptions.asPaint()
            val text = paintOptions.text

            if (text == null) {
                canvas.drawPath(myPath, paint)
            } else {
                val textStartPoint = paintOptions.textStartPoint!!
                val x = textStartPoint.x
                val y = textStartPoint.y
                canvas.drawText(text, x, y, paint)
            }
        }

        val paint = paintOptions.asPaint()
        val text = paintOptions.text

        if (text == null) {
            canvas.drawPath(path, paint)
        }
    }

    fun clearCanvas() {
        lastPaths = paths.clone() as LinkedHashMap<MyPath, PaintOptions>
        path.reset()
        paths.clear()
        invalidate()
    }

    private fun actionDown(x: Float, y: Float) {
        path.reset()
        path.moveTo(x, y)
        curX = x
        curY = y
    }

    private fun actionMove(x: Float, y: Float) {
        path.quadTo(curX, curY, (x + curX) / 2, (y + curY) / 2)
        curX = x
        curY = y
    }

    private fun actionUp() {
        path.lineTo(curX, curY)

        // draw a dot on click
        if (startX == curX && startY == curY) {
            path.lineTo(curX, curY + 2)
            path.lineTo(curX + 1, curY + 2)
            path.lineTo(curX + 1, curY)
        }

        paths[path] = paintOptions
        path = MyPath()
        paintOptions = PaintOptions(paintOptions.color, paintOptions.strokeWidth, paintOptions.alpha, paintOptions.isEraserOn)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = x
                startY = y
                actionDown(x, y)
                undonePaths.clear()
            }
            MotionEvent.ACTION_MOVE -> actionMove(x, y)
            MotionEvent.ACTION_UP -> actionUp()
        }

        invalidate()

        return true
    }
}