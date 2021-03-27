package com.divyanshu.draw.util

import android.widget.ImageView
import com.bumptech.glide.Glide

object ImageUtils {
    fun load(urlString: String, imageView: ImageView) {
        val context = imageView.context
        Glide.with(context) //
                .load(urlString) //
                .into(imageView)
    }
}