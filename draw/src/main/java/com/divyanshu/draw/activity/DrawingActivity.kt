package com.divyanshu.draw.activity

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
    }

    private var mostRecentlySelectedAlpha: Int = 255

    private var mostRecentlySelectedColorInt: Int? = null

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
        circle_view_opacity.setCircleRadius(100f)

        image_draw_eraser.setOnClickListener {
            val newIsEraserOn = !draw_view.isEraserOn
            draw_view.isEraserOn = newIsEraserOn
            image_draw_eraser.isSelected = newIsEraserOn
            toggleDrawTools(draw_tools, false)
        }

        image_draw_eraser.setOnLongClickListener {
            draw_view.clearCanvas()
            toggleDrawTools(draw_tools, false)

            true
        }

        image_draw_width.setOnClickListener {
            if (draw_tools.translationY == (56).toPx) {
                toggleDrawTools(draw_tools, true)
            } else if (draw_tools.translationY == (0).toPx && seekBar_width.visibility == View.VISIBLE) {
                toggleDrawTools(draw_tools, false)
            }

            circle_view_width.visibility = View.VISIBLE
            circle_view_opacity.visibility = View.GONE
            seekBar_width.visibility = View.VISIBLE
            seekBar_opacity.visibility = View.GONE
            draw_color_palette.visibility = View.GONE
        }

        image_draw_opacity.setOnClickListener {
            if (draw_tools.translationY == (56).toPx) {
                toggleDrawTools(draw_tools, true)
            } else if (draw_tools.translationY == (0).toPx && seekBar_opacity.visibility == View.VISIBLE) {
                toggleDrawTools(draw_tools, false)
            }

            circle_view_width.visibility = View.GONE
            circle_view_opacity.visibility = View.VISIBLE
            seekBar_width.visibility = View.GONE
            seekBar_opacity.visibility = View.VISIBLE
            draw_color_palette.visibility = View.GONE
        }

        image_draw_color.setOnClickListener {
            if (draw_tools.translationY == (56).toPx) {
                toggleDrawTools(draw_tools, true)
            } else if (draw_tools.translationY == (0).toPx && draw_color_palette.visibility == View.VISIBLE) {
                toggleDrawTools(draw_tools, false)
            }

            circle_view_width.visibility = View.GONE
            circle_view_opacity.visibility = View.GONE
            seekBar_width.visibility = View.GONE
            seekBar_opacity.visibility = View.GONE
            draw_color_palette.visibility = View.VISIBLE
        }

        image_draw_text.setOnClickListener {
            toggleDrawTools(draw_tools, false)

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
            toggleDrawTools(draw_tools, false)
        }

        image_draw_redo.setOnClickListener {
            draw_view.redo()
            toggleDrawTools(draw_tools, false)
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

    private fun toggleDrawTools(view: View, showView: Boolean = true) {
        if (showView) {
            view.animate().translationY((0).toPx)
        } else {
            view.animate().translationY((56).toPx)
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

    private val Int.toPx: Float
        get() = (this * Resources.getSystem().displayMetrics.density)
}