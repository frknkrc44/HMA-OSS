package org.frknkrc44.hma_oss.ui.util

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Rect
import android.graphics.drawable.Drawable

class FixedSizeDrawable(private val drawable: Drawable, private val sizePx: Int) : Drawable() {

    override fun getIntrinsicWidth() = sizePx

    override fun getIntrinsicHeight() = sizePx

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        drawable.bounds = bounds
    }

    override fun draw(p0: Canvas) {
        drawable.draw(p0)
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun getOpacity() = drawable.opacity

    override fun setAlpha(p0: Int) {
        drawable.alpha = p0
    }

    override fun setColorFilter(p0: ColorFilter?) {
        drawable.colorFilter = p0
    }
}
