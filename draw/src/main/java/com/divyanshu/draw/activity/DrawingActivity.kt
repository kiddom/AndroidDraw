package com.divyanshu.draw.activity

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.divyanshu.draw.R
import com.divyanshu.draw.result.contract.CreateDrawingActivityResultContract
import com.divyanshu.draw.util.ImageUtils
import com.divyanshu.draw.util.TooltipUtils
import kotlinx.android.synthetic.main.activity_drawing.*
import kotlinx.android.synthetic.main.color_palette_view.*
import java.io.ByteArrayOutputStream

class DrawingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_drawing)

        setBackgroundIfNeeded()

        image_close_drawing.setOnClickListener {
            finish()
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

    private fun setUpDrawTools() {
        circle_view_opacity.setCircleRadius(100f)

        image_draw_eraser.setOnClickListener {
            draw_view.toggleEraser()
            image_draw_eraser.isSelected = draw_view.isEraserOn
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
            val color = ResourcesCompat.getColor(resources, R.color.color_black, null)
            draw_view.setColor(color)
            circle_view_opacity.setColor(color)
            circle_view_width.setColor(color)
            scaleColorView(image_color_black)
        }

        image_color_red.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_red, null)
            draw_view.setColor(color)
            circle_view_opacity.setColor(color)
            circle_view_width.setColor(color)
            scaleColorView(image_color_red)
        }

        image_color_yellow.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_yellow, null)
            draw_view.setColor(color)
            circle_view_opacity.setColor(color)
            circle_view_width.setColor(color)
            scaleColorView(image_color_yellow)
        }

        image_color_green.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_green, null)
            draw_view.setColor(color)
            circle_view_opacity.setColor(color)
            circle_view_width.setColor(color)
            scaleColorView(image_color_green)
        }

        image_color_blue.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_blue, null)
            draw_view.setColor(color)
            circle_view_opacity.setColor(color)
            circle_view_width.setColor(color)
            scaleColorView(image_color_blue)
        }

        image_color_pink.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_pink, null)
            draw_view.setColor(color)
            circle_view_opacity.setColor(color)
            circle_view_width.setColor(color)
            scaleColorView(image_color_pink)
        }

        image_color_brown.setOnClickListener {
            val color = ResourcesCompat.getColor(resources, R.color.color_brown, null)
            draw_view.setColor(color)
            circle_view_opacity.setColor(color)
            circle_view_width.setColor(color)
            scaleColorView(image_color_brown)
        }
    }

    private fun scaleColorView(view: View) {
        //reset scale of all views
        image_color_black.scaleX = 1f
        image_color_black.scaleY = 1f

        image_color_red.scaleX = 1f
        image_color_red.scaleY = 1f

        image_color_yellow.scaleX = 1f
        image_color_yellow.scaleY = 1f

        image_color_green.scaleX = 1f
        image_color_green.scaleY = 1f

        image_color_blue.scaleX = 1f
        image_color_blue.scaleY = 1f

        image_color_pink.scaleX = 1f
        image_color_pink.scaleY = 1f

        image_color_brown.scaleX = 1f
        image_color_brown.scaleY = 1f

        //set scale of selected view
        view.scaleX = 1.5f
        view.scaleY = 1.5f
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
                draw_view.setAlpha(progress)
                circle_view_opacity.setAlpha(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private val Int.toPx: Float
        get() = (this * Resources.getSystem().displayMetrics.density)
}