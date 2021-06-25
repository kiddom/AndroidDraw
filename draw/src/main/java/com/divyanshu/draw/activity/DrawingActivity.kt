package com.divyanshu.draw.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.DragEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.annotation.ColorRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.*
import com.divyanshu.draw.R
import com.divyanshu.draw.result.contract.CreateDrawingActivityResultContract
import com.divyanshu.draw.util.ImageUtils
import com.divyanshu.draw.util.TooltipUtils
import kotlinx.android.synthetic.main.activity_drawing.*
import kotlinx.android.synthetic.main.color_palette_view.*
import java.io.ByteArrayOutputStream

class DrawingActivity : AppCompatActivity() {
    companion object {
        private const val TOP_MARGIN_FUDGE_FACTOR = -10
        private const val MINIMUM_SCALE_FACTOR = 1f
        private const val MAXIMUM_SCALE_FACTOR = 10f
    }

    private var currentScaleFactor: Float = 1.0f

    private var mostRecentlySelectedAlpha: Int = 255

    private var mostRecentlySelectedColorInt: Int? = null

    private lateinit var scaleGestureDetector: ScaleGestureDetector

    override fun onBackPressed() {
        AlertDialog.Builder(this).run {
            setMessage(R.string.leave_without_saving)
            setNegativeButton(android.R.string.cancel, null)
            setPositiveButton(R.string.leave) { _, _ ->
                super.onBackPressed()
            }
            show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_drawing)

        mostRecentlySelectedColorInt = ResourcesCompat.getColor(resources, R.color.color_black, null)

        val simpleOnScaleGestureListener = SimpleOnScaleGestureListener()
        scaleGestureDetector = ScaleGestureDetector(this, simpleOnScaleGestureListener)

        setBackgroundIfNeeded()

        image_close_drawing.setOnClickListener {
            onBackPressed()
        }

        fab_send_drawing.setOnClickListener {
            val bStream = ByteArrayOutputStream()
            val bitmap = draw_view.getBitmap()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bStream)
            val byteArray = bStream.toByteArray()
            val returnIntent = Intent()
            returnIntent.putExtra("bitmap", byteArray)
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }

        setUpDrawCanvas()
        setUpPanAndScaleListenerView()
        setUpDrawTools()
        setUpAddTextContainer()
        colorSelector()
        setPaintAlpha()
        setPaintWidth()
        setUpTooltipTexts()
    }

    private fun setBackgroundIfNeeded() {
        val extras = intent.extras!!
        val backgroundImageUrlStringProvided = extras.containsKey(CreateDrawingActivityResultContract.BACKGROUND_IMAGE_URL_STRING_EXTRA_KEY)

        if (backgroundImageUrlStringProvided) {
            val backgroundImageUrlString = extras.getString(CreateDrawingActivityResultContract.BACKGROUND_IMAGE_URL_STRING_EXTRA_KEY)!!

            ImageUtils.load(backgroundImageUrlString, background)
        } else {
            val layoutParams = background.layoutParams as ConstraintLayout.LayoutParams

            with(layoutParams) {
                height = 0
                width = 0
            }

            background.layoutParams = layoutParams
        }
    }

    private fun setUpDrawCanvas() {
        val extras = intent.extras!!
        val canvasColorProvided = extras.containsKey(CreateDrawingActivityResultContract.CANVAS_COLOR_EXTRA_KEY)

        if (canvasColorProvided) {
            val canvasColor = extras.getInt(CreateDrawingActivityResultContract.CANVAS_COLOR_EXTRA_KEY)

            draw_view.canvasColor = canvasColor
        }
    }

    private fun setUpAddTextContainer() {
        add_text_container.isInvisible = true
    }

    private fun setUpDrawTools() {
        image_draw_pan_and_scale.setOnClickListener {
            updateSelectedState(it)

            pan_and_scale_listener.isVisible = it.isSelected
        }

        with(erase_all) {
            isGone = true

            setOnClickListener {
                draw_view.clearCanvas()
            }
        }

        circle_view_opacity.setCircleRadius(100f)

        image_draw_eraser.setOnClickListener {
            updateSelectedState(it)

            val selected = it.isSelected
            toggleDrawTools(selected)

            toggleAuxiliaryViews(erase_all)

            draw_view.isEraserOn = selected
        }

        image_draw_width.setOnClickListener {
            updateSelectedState(it)
            toggleDrawTools(it.isSelected)
            toggleAuxiliaryViews(circle_view_width, seekBar_width)
        }

        image_draw_opacity.setOnClickListener {
            updateSelectedState(it)
            toggleDrawTools(it.isSelected)
            toggleAuxiliaryViews(circle_view_opacity, seekBar_opacity)
            draw_view.isEraserOn = false
        }

        image_draw_color.setOnClickListener {
            updateSelectedState(it)
            toggleDrawTools(it.isSelected)
            toggleAuxiliaryViews(draw_color_palette)
            draw_view.isEraserOn = false
        }

        image_draw_text.setOnClickListener {
            it.isEnabled = false

            add_text_container.isVisible = true
            val addText = layoutInflater.inflate(R.layout.add_text, add_text_container, false)
            add_text_container.addView(addText)

            addText.updateLayoutParams<RelativeLayout.LayoutParams> {
                val widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                addText.measure(widthMeasureSpec, heightMeasureSpec)

                val addTextContainerWidth = add_text_container.width
                val addTextWidth = addText.measuredWidth
                val newMarginStart = (addTextContainerWidth - addTextWidth) / 2
                marginStart = newMarginStart

                val addTextContainerHeight = add_text_container.height
                val addTextHeight = addText.measuredHeight
                val newTopMargin = (addTextContainerHeight - addTextHeight) / 2
                topMargin = newTopMargin
            }

            val moveView = addText.findViewById<View>(R.id.move)
            var touchX = 0
            var touchY = 0
            val onTouchListener: (View, MotionEvent) -> Boolean = { v, event ->
                touchX = event.x.toInt()
                touchY = event.y.toInt()

                v.performClick()

                moveView.setOnTouchListener(null)

                false
            }
            moveView.setOnTouchListener(onTouchListener)

            add_text_container.setOnDragListener { _, dragEvent ->
                val action = dragEvent.action

                when (action) {
                    DragEvent.ACTION_DROP -> {
                        val newX = dragEvent.x.toInt()
                        val newMarginStart = newX - touchX
                        val newY = dragEvent.y.toInt()
                        val newTopMargin = newY - touchY

                        addText.updateLayoutParams<RelativeLayout.LayoutParams> {
                            marginStart = newMarginStart
                            topMargin = newTopMargin
                        }

                        true
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        addText.isVisible = true
                        moveView.setOnTouchListener(onTouchListener)

                        true
                    }
                    DragEvent.ACTION_DRAG_ENTERED -> true
                    DragEvent.ACTION_DRAG_EXITED -> true
                    DragEvent.ACTION_DRAG_LOCATION -> true
                    DragEvent.ACTION_DRAG_STARTED -> true
                    else -> false
                }
            }

            val onClickListener: (View) -> Unit = {
                addText.isInvisible = true

                val clipData = ClipData.newPlainText("", "")
                val dragShadowBuilder = object : View.DragShadowBuilder(addText) {
                    override fun onProvideShadowMetrics(
                            outShadowSize: Point, outShadowTouchPoint: Point
                    ) {
                        super.onProvideShadowMetrics(outShadowSize, outShadowTouchPoint)

                        outShadowTouchPoint.set(touchX, touchY)
                    }
                }
                val flags = 0
                ViewCompat.startDragAndDrop(addText, clipData, dragShadowBuilder, null, flags)
            }
            moveView.setOnClickListener(onClickListener)

            val saveImageButton = addText.findViewById<ImageButton>(R.id.save)

            saveImageButton.setOnClickListener {
                val moveViewWidth = moveView.measuredWidth
                val editText = addText.findViewById<EditText>(R.id.edit_text)
                val text = editText.text.toString()
                val textSize = editText.textSize
                val addTextMarginStart = addText.marginStart.toFloat()
                val editTextPaddingStart = editText.paddingStart
                val x = moveViewWidth + addTextMarginStart + editTextPaddingStart
                val addTextMarginTop = addText.marginTop.toFloat()
                val addTextHeight = addText.height
                val editTextPaddingTop = editText.paddingTop
                val y = addTextMarginTop + (addTextHeight / 2) + editTextPaddingTop + TOP_MARGIN_FUDGE_FACTOR
                draw_view.addText(text, textSize, x, y)

                with(add_text_container) {
                    removeAllViews()
                    isInvisible = true
                }

                image_draw_text.isEnabled = true
                draw_view.isEraserOn = false
            }

            val editText = addText.findViewById<EditText>(R.id.edit_text)

            with(editText) {
                val alphaFloat = mostRecentlySelectedAlpha / 255f
                alpha = alphaFloat

                val mostRecentlySelectedColorInt = mostRecentlySelectedColorInt!!
                setTextColor(mostRecentlySelectedColorInt)

                requestFocus()
            }

            with(editText) {
                val inputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                requestFocus()
            }
        }

        image_draw_undo.setOnClickListener {
            draw_view.undo()
        }

        image_draw_redo.setOnClickListener {
            draw_view.redo()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpPanAndScaleListenerView() {
        with(pan_and_scale_listener) {
            isGone = true

            setOnTouchListener { v, event ->
                scaleGestureDetector.onTouchEvent(event)
            }
        }
    }

    private fun setUpTooltipTexts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            TooltipUtils.setTooltipText(image_close_drawing, R.string.close)
            TooltipUtils.setTooltipText(fab_send_drawing, R.string.save)
            TooltipUtils.setTooltipText(image_draw_eraser, R.string.eraser)
            TooltipUtils.setTooltipText(image_draw_width, R.string.stroke_width)
            TooltipUtils.setTooltipText(image_draw_color, R.string.color)
            TooltipUtils.setTooltipText(image_draw_opacity, R.string.opacity)
            TooltipUtils.setTooltipText(image_draw_undo, R.string.undo)
            TooltipUtils.setTooltipText(image_draw_redo, R.string.redo)
        }
    }

    private fun colorSelector() {
        image_color_black.setOnClickListener {
            selectColor(R.color.color_black, it)
        }

        image_color_red.setOnClickListener {
            selectColor(R.color.color_red, it)
        }

        image_color_yellow.setOnClickListener {
            selectColor(R.color.color_yellow, it)
        }

        image_color_green.setOnClickListener {
            selectColor(R.color.color_green, it)
        }

        image_color_blue.setOnClickListener {
            selectColor(R.color.color_blue, it)
        }

        image_color_pink.setOnClickListener {
            selectColor(R.color.color_pink, it)
        }

        image_color_brown.setOnClickListener {
            selectColor(R.color.color_brown, it)
        }

        image_color_white.setOnClickListener {
            selectColor(R.color.color_white, it)
        }
    }

    private fun scaleColorView(view: View) {
        setOf(image_color_black, image_color_blue, image_color_brown, image_color_green,
                image_color_pink, image_color_red, image_color_white, image_color_yellow).forEach {
            it.scaleX = 1f
            it.scaleY = 1f
        }

        //set scale of selected view
        view.scaleX = 1.5f
        view.scaleY = 1.5f
    }

    private fun selectColor(@ColorRes colorResId: Int, colorImageView: View) {
        val colorInt = ResourcesCompat.getColor(resources, colorResId, null)
        mostRecentlySelectedColorInt = colorInt

        val editText = findViewById<EditText>(R.id.edit_text)
        editText?.setTextColor(colorInt)

        draw_view.setColor(colorInt)
        circle_view_opacity.setColor(colorInt)
        circle_view_width.setColor(colorInt)
        scaleColorView(colorImageView)
    }

    private fun setPaintWidth() {
        seekBar_width.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                draw_view.setStrokeWidth(progress.toFloat())
                circle_view_width.setCircleRadius(progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setPaintAlpha() {
        seekBar_opacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val alpha = progress * 255 / 100
                mostRecentlySelectedAlpha = alpha

                val editText = findViewById<EditText>(R.id.edit_text)

                if (editText != null) {
                    val alphaFloat = progress / 100f
                    editText.alpha = alphaFloat
                }

                draw_view.setAlpha(alpha)
                circle_view_opacity.setAlpha(alpha)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun toggleAuxiliaryViews(vararg viewsToShow: View) {
        setOf(circle_view_opacity, circle_view_width, draw_color_palette, erase_all, seekBar_opacity, seekBar_width).forEach {
            it.isInvisible = !viewsToShow.contains(it)
        }
    }

    private fun toggleDrawTools(showView: Boolean) {
        val translationY = if (showView) {
            0
        } else {
            56
        }

        draw_tools.animate().translationY(translationY.toPx)
    }

    private fun updateSelectedState(selectedView: View) {
        val toolViews = setOf(image_draw_color, image_draw_eraser, image_draw_opacity,
                image_draw_pan_and_scale, image_draw_width)

        toolViews.forEach {
            it.isSelected = (it == selectedView && !it.isSelected)
        }
    }

    private val Int.toPx: Float
        get() = (this * Resources.getSystem().displayMetrics.density)

    private inner class SimpleOnScaleGestureListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
            currentScaleFactor *= scaleGestureDetector.scaleFactor
            currentScaleFactor = MINIMUM_SCALE_FACTOR.coerceAtLeast(currentScaleFactor.coerceAtMost(MAXIMUM_SCALE_FACTOR))
            draw_view.scaleX = currentScaleFactor
            draw_view.scaleY = currentScaleFactor

            return true
        }
    }
}