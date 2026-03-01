package top.secret.hma.v1.ui.util

import android.widget.Toast
import androidx.annotation.StringRes
import top.secret.hma.v1.hmaApp

fun showToast(@StringRes resId: Int) {
    Toast.makeText(hmaApp, resId, Toast.LENGTH_SHORT).show()
}

fun showToast(text: CharSequence) {
    Toast.makeText(hmaApp, text, Toast.LENGTH_SHORT).show()
}
