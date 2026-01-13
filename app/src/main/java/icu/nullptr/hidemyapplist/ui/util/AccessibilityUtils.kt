package icu.nullptr.hidemyapplist.ui.util

import android.content.ContentResolver
import android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
import android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE
import android.provider.Settings.Global.WINDOW_ANIMATION_SCALE
import android.provider.Settings.Global.getFloat

class AccessibilityUtils {
    companion object {
        fun isAnimationEnabled(cr: ContentResolver): Boolean {
            return getFloat(cr, ANIMATOR_DURATION_SCALE, 1.0f) > 0.0f
                && getFloat(cr, TRANSITION_ANIMATION_SCALE, 1.0f) > 0.0f
                && getFloat(cr, WINDOW_ANIMATION_SCALE, 1.0f) > 0.0f
        }
    }
}
