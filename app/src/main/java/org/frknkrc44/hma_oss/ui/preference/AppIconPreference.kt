package org.frknkrc44.hma_oss.ui.preference

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import icu.nullptr.hidemyapplist.data.AppConstants.allAppIcons
import icu.nullptr.hidemyapplist.service.PrefManager
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.asDrawable
import icu.nullptr.hidemyapplist.util.PackageHelper.findEnabledAppComponent
import org.frknkrc44.hma_oss.BuildConfig
import org.frknkrc44.hma_oss.R


@Suppress("deprecation")
class AppIconPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    var viewHolder: PreferenceViewHolder? = null

    @SuppressLint("SetTextI18n")
    @Deprecated("Deprecated in Java")
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        viewHolder = holder

        super.onBindViewHolder(holder)

        updateHolder()
    }

    fun updateHolder() {
        if (viewHolder == null) return

        (viewHolder!!.itemView as ViewGroup).apply {
            val summary = findViewById<View>(android.R.id.summary)
            val parent = summary.parent as ViewGroup
            parent.removeView(summary)

            val view = LayoutInflater.from(context).inflate(R.layout.preference_app_icon, parent, false)
            view.id = android.R.id.summary
            (view.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.BELOW, android.R.id.title)

            val appIconSelector: RadioGroup = view.findViewById(R.id.app_icon_selector)

            for (idx in 0 ..< allAppIcons.size) {
                val radioButton = object : AppCompatRadioButton(context) {
                    override fun setChecked(checked: Boolean) {
                        if (PrefManager.hideIcon) {
                            alpha = 0.4f
                            return
                        }

                        super.setChecked(checked)

                        alpha = if (checked) 1.0f else 0.4f
                    }
                }

                with(radioButton) {
                    layoutParams = RadioGroup.LayoutParams(-2, -2).apply {
                        val padding = context.resources.getDimensionPixelOffset(R.dimen.item_padding_mini2x)
                        setMargins(padding, padding, padding, padding)
                    }

                    id = idx
                    gravity = Gravity.CENTER_VERTICAL
                    buttonDrawable = allAppIcons[idx].first.asDrawable(context)
                    text = ""
                    buttonTintList = null
                }

                appIconSelector.addView(radioButton)
            }

            val selected = findEnabledAppComponent(context)
            if (selected != null) {
                appIconSelector.check(allAppIcons.indexOfFirst { it.second == selected.className })
            }

            appIconSelector.setOnCheckedChangeListener { _, checkedId ->
                setEnabledComponent(allAppIcons[checkedId].second)
            }

            parent.addView(view)
        }
    }

    private fun disableAppIcon() {
        val enabled = findEnabledAppComponent(context)
        if (enabled != null) {
            context.packageManager.setComponentEnabledSetting(
                enabled,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun setEnabledComponent(className: String) {
        disableAppIcon()

        context.packageManager.setComponentEnabledSetting(
            ComponentName(BuildConfig.APPLICATION_ID, className),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
