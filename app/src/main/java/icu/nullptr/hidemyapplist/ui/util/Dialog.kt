package icu.nullptr.hidemyapplist.ui.util

import android.annotation.SuppressLint
import android.os.CountDownTimer
import androidx.appcompat.app.AlertDialog

fun AlertDialog.withDisableButton(disabledButtonId: Int = AlertDialog.BUTTON_POSITIVE, seconds: Int = 5): AlertDialog {
    val tick = 1000L
    var countDownTimer: CountDownTimer? = null

    setOnDismissListener {
        countDownTimer?.cancel()
    }

    setOnShowListener {
        val disabledBtn = getButton(disabledButtonId)
        disabledBtn.isEnabled = false

        countDownTimer = object : CountDownTimer(seconds * tick, tick) {
            val btnText = disabledBtn.text

            override fun onFinish() {
                disabledBtn.text = btnText
                disabledBtn.isEnabled = true
            }

            @SuppressLint("SetTextI18n")
            override fun onTick(p0: Long) {
                val secondsLeft = (p0 / tick) + 1

                disabledBtn.text = "$btnText ($secondsLeft)"
            }
        }

        countDownTimer.start()
    }

    return this
}
