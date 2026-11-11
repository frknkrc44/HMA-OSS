package icu.nullptr.hidemyapplist.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceFragmentCompat
import icu.nullptr.hidemyapplist.ui.util.applyBottomWindowInsets

abstract class EdgeToEdgePreferenceFragment : PreferenceFragmentCompat() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.clipToPadding = false
        listView.applyBottomWindowInsets()
    }
}
