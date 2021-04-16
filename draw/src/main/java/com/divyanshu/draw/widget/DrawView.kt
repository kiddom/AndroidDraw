package com.divyanshu.draw.widget

import android.content.Context
import android.graphics.*
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

    private var paint = Paint()
    private var path = MyPath()
    private var paintOptions = PaintOptions()

    private var curX = 0f
    private var curY = 0f
    private var startX = 0f
    private var startY = 0f
    private var isSaving = false
    private var isStrokeWidthBarEnabled = false

    var isEraserOn = false
        private set

    init {
        paint.apply {
            color = paintOptions.color
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = paintOptions.strokeWidth
            isAntiAlias = true
        }
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

    fun setAlpha(newAlpha: Int) {
        val alpha = (newAlpha * 255) / 100
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

        for ((key, value) in paths) {
            changePaint(value)
            canvas.drawPath(key, paint)
        }

        changePaint(paintOptions)
        canvas.drawPath(path, paint)
    }

    private fun changePaint(paintOptions: PaintOptions) {
        if (paintOptions.isEraserOn) {
            paint.color = Color.TRANSPARENT
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        } else {
            paint.color = paintOptions.color
            paint.xfermode = null
        }

        paint.strokeWidth = paintOptions.strokeWidth
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

    fun toggleEraser() {
        isEraserOn = !isEraserOn
        paintOptions = paintOptions.copy(isEraserOn = isEraserOn)
        invalidate()
    }
}