package com.divyanshu.draw.result.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import com.divyanshu.draw.BuildConfig
import com.divyanshu.draw.activity.DrawingActivity

object CreateDrawingActivityResultContract : ActivityResultContract<String?, ByteArray?>() {
    private const val EXTRA_KEY_PREFIX = BuildConfig.LIBRARY_PACKAGE_NAME + "."
    const val BACKGROUND_IMAGE_URL_STRING_EXTRA_KEY = EXTRA_KEY_PREFIX + "BackgroundImageUrlString"

    override fun createIntent(context: Context, backgroundImageUrlString: String?): Intent {
        val intent = Intent(context, DrawingActivity::class.java).apply {
            if (backgroundImageUrlString != null) {
                val bundle = Bundle().apply {
                    putString(BACKGROUND_IMAGE_URL_STRING_EXTRA_KEY, backgroundImageUrlString)
                }

                putExtras(bundle)
            }
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