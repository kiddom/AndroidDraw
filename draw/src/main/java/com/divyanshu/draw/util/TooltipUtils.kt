package com.divyanshu.draw.util

import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat

object TooltipUtils {
    fun setTooltipText(view: View, @StringRes tooltipTextStringResId: Int) {
        val context = view.context
        val tooltipText = context.getText(tooltipTextStringResId)

        TooltipCompat.setTooltipText(view, tooltipText)
    }
}