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
import icu.nullptr.hidemyapplist.data.AppConstants
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.asDrawable
import icu.nullptr.hidemyapplist.util.PackageHelper.findEnabledAppComponent
import org.frknkrc44.hma_oss.BuildConfig
import org.frknkrc44.hma_oss.R


@Suppress("deprecation")
class AppIconPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    val appIconsList = listOf(
        R.mipmap.ic_launcher.asDrawable(context),
        R.mipmap.ic_launcher_alt.asDrawable(context),
        R.mipmap.ic_launcher_alt_2.asDrawable(context),
        R.mipmap.ic_launcher_alt_3.asDrawable(context),
    )

    val allAppIcons = listOf(
        ComponentName(BuildConfig.APPLICATION_ID, AppConstants.COMPONENT_NAME_DEFAULT),
        ComponentName(BuildConfig.APPLICATION_ID, AppConstants.COMPONENT_NAME_ALT),
        ComponentName(BuildConfig.APPLICATION_ID, AppConstants.COMPONENT_NAME_ALT_2),
        ComponentName(BuildConfig.APPLICATION_ID, AppConstants.COMPONENT_NAME_ALT_3),
    )

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

            val selected = findEnabledAppComponent(context)
            val selectedIdx = allAppIcons.indexOfFirst { it.className == selected?.className }

            for (idx in 0 ..< appIconsList.size) {
                val radioButton = object : AppCompatRadioButton(context) {
                    override fun setChecked(checked: Boolean) {
                        super.setChecked(checked)

                        alpha = if (checked) 1.0f else 0.4f

                        if (checked) {
                            setEnabledComponent(allAppIcons[idx])
                        }
                    }
                }

                radioButton.layoutParams = RadioGroup.LayoutParams(-2, -2).apply {
                    val padding = context.resources.getDimensionPixelOffset(R.dimen.item_padding_mini2x)
                    setMargins(padding, padding, padding, padding)
                }

                radioButton.gravity = Gravity.CENTER_VERTICAL
                radioButton.id = idx
                radioButton.isChecked = idx == selectedIdx

                radioButton.buttonDrawable = appIconsList.elementAt(idx)
                radioButton.text = ""
                radioButton.buttonTintList = null

                appIconSelector.addView(radioButton)
            }

            appIconSelector.check(selectedIdx)

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

    private fun setEnabledComponent(componentName: ComponentName) {
        disableAppIcon()

        context.packageManager.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}