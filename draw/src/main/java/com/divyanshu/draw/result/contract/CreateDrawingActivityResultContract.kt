package com.divyanshu.draw.result.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import com.divyanshu.draw.BuildConfig
import com.divyanshu.draw.activity.DrawingActivity

object CreateDrawingActivityResultContract : ActivityResultContract<List<Any?>, ByteArray?>() {
    private const val EXTRA_KEY_PREFIX = BuildConfig.LIBRARY_PACKAGE_NAME + "."
    const val BACKGROUND_IMAGE_URL_STRING_EXTRA_KEY = EXTRA_KEY_PREFIX + "BackgroundImageUrlString"
    const val CANVAS_COLOR_EXTRA_KEY = EXTRA_KEY_PREFIX + "CanvasColor"

    fun launch(activityResultLauncher: ActivityResultLauncher<List<Any?>>, backgroundImageUrlString: String? = null, @ColorInt canvasColor: Int? = null) {
        val input = listOf(backgroundImageUrlString, canvasColor)
        activityResultLauncher.launch(input)
    }

    override fun createIntent(context: Context, input: List<Any?>): Intent {
        val intent = Intent(context, DrawingActivity::class.java).apply {
            val bundle = Bundle().apply {
                val backgroundImageUrlString = input[0] as String?
                val backgroundImageUrlStringProvided = backgroundImageUrlString != null

                if(backgroundImageUrlStringProvided) {
                    putString(BACKGROUND_IMAGE_URL_STRING_EXTRA_KEY, backgroundImageUrlString)
                }

                val canvasColor = input[1] as Int?
                val canvasColorProvided = canvasColor != null

                if (canvasColorProvided) {
                    putInt(CANVAS_COLOR_EXTRA_KEY, canvasColor!!)
                }
            }

            putExtras(bundle)
        }

        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ByteArray? {
        val result = if (intent == null || resultCode != Activity.RESULT_OK) {
            null
        } else {
            intent.getByteArrayExtra("bitmap")!!
        }

        return result
    }
}