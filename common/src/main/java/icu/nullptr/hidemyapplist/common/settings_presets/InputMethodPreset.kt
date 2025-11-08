package icu.nullptr.hidemyapplist.common.settings_presets

import android.provider.Settings
import icu.nullptr.hidemyapplist.common.Constants

class InputMethodPreset : BasePreset(NAME) {
    companion object {
        const val NAME = "input_method"
    }

    override val settingsKVPairs = listOf(
        ReplacementItem(
            Settings.Secure.DEFAULT_INPUT_METHOD,
            "com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME",
            Constants.SETTINGS_SECURE,
        ),
        ReplacementItem(
            Settings.Secure.TTS_DEFAULT_SYNTH,
            "com.google.android.tts",
            Constants.SETTINGS_SECURE,
        ),
    )
}