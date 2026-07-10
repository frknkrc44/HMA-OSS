package icu.nullptr.hidemyapplist.ui.util

import android.widget.Toast
import androidx.annotation.StringRes
import icu.nullptr.hidemyapplist.MyApp.Companion.hmaApp

fun showToast(@StringRes resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(hmaApp, resId, duration).show()
}

fun showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(hmaApp, text, duration).show()
}
