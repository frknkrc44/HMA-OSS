package icu.nullptr.hidemyapplist.ui.util

import android.widget.Toast
import androidx.annotation.StringRes
import icu.nullptr.hidemyapplist.MyApp.Companion.hmaApp

fun showToast(@StringRes resId: Int) {
    Toast.makeText(hmaApp, resId, Toast.LENGTH_SHORT).show()
}

fun showToast(text: CharSequence) {
    Toast.makeText(hmaApp, text, Toast.LENGTH_SHORT).show()
}
