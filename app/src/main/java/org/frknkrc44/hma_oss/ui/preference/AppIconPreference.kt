package org.frknkrc44.hma_oss.ui.preference

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
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
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.asColor
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.asDrawable
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.themeColor
import icu.nullptr.hidemyapplist.util.PackageHelper.findEnabledAppComponent
import org.frknkrc44.hma_oss.BuildConfig
import org.frknkrc44.hma_oss.R

@Suppress("ReplaceManualRangeWithIndicesCalls")
class AppIconPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        (holder.itemView as ViewGroup).apply {
            val summary = findViewById<View>(android.R.id.summary)
            val parent = summary.parent as ViewGroup
            parent.removeView(summary)

            val view = LayoutInflater.from(context).inflate(R.layout.preference_app_icon, parent, false)
            view.id = android.R.id.summary
            (view.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.BELOW, android.R.id.title)

            val appIconSelector: RadioGroup = view.findViewById(R.id.app_icon_selector)

            for (idx in 0 ..< allAppIcons.size) {
                val radioButton = object : AppCompatRadioButton(context) {
                    init {
                        layoutParams = RadioGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        ).apply {
                            val padding = context.resources.getDimensionPixelOffset(R.dimen.item_padding_mini2x)
                            setMargins(padding, padding, padding, padding)
                        }

                        id = idx
                        gravity = Gravity.CENTER_VERTICAL
                        text = ""
                        buttonDrawable = allAppIcons[idx].first.asDrawable(context)
                        buttonTintList = null
                        buttonTintMode = PorterDuff.Mode.SRC_ATOP
                    }

                    override fun setChecked(checked: Boolean) {
                        if (PrefManager.hideIcon) {
                            foreground = null
                            alpha = 0.4f
                            return
                        }

                        super.setChecked(checked)

                        buttonTintList = if (isChecked) {
                            ColorStateList.valueOf(
                                (context.themeColor(
                                    androidx.appcompat.R.attr.colorPrimaryDark
                                ) - 0x88000000).toInt()
                            )
                        } else {
                            null
                        }

                        foreground = if (checked) R.drawable.check_24px.asDrawable(context) else null
                        alpha = if (checked) 1.0f else 0.4f
                    }
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
