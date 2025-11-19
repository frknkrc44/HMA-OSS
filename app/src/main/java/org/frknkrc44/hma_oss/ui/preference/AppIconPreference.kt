package org.frknkrc44.hma_oss.ui.preference

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.graphics.drawable.updateBounds
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import icu.nullptr.hidemyapplist.data.AppConstants
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.asDrawable
import icu.nullptr.hidemyapplist.ui.util.asComponentName
import icu.nullptr.hidemyapplist.util.PackageHelper.findEnabledAppComponent
import org.frknkrc44.hma_oss.BuildConfig
import org.frknkrc44.hma_oss.R


@Suppress("deprecation")
class AppIconPreference : Preference {
    val appIconsList = mutableMapOf<String, Drawable?>()
    lateinit var appIconSelector: RadioGroup
    val allAppIcons = listOf(
        ComponentName(BuildConfig.APPLICATION_ID, AppConstants.COMPONENT_NAME_DEFAULT),
        ComponentName(BuildConfig.APPLICATION_ID, AppConstants.COMPONENT_NAME_ALT),
        ComponentName(BuildConfig.APPLICATION_ID, AppConstants.COMPONENT_NAME_ALT_2),
    )

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    @SuppressLint("SetTextI18n")
    @Deprecated("Deprecated in Java")
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val inflater = LayoutInflater.from(context)

        (holder.itemView as ViewGroup).apply {
            removeAllViews()
            addView(inflater.inflate(R.layout.preference_app_icon, this, false))
        }

        appIconSelector = holder.findViewById(R.id.app_icon_selector) as RadioGroup

        val selected = findEnabledAppComponent(context)
        val selectedIdx = allAppIcons.indexOfFirst { it.className == selected?.className } + 1

        for (idx in 0 ..< appIconsList.size) {
            val radioButton = object : AppCompatRadioButton(context) {
                override fun setChecked(checked: Boolean) {
                    super.setChecked(checked)

                    alpha = if (checked) 1.0f else 0.4f

                    if (checked) {
                        if (idx == 0) {
                            disableAppIcon()
                        } else {
                            setEnabledComponent(allAppIcons[idx - 1])
                        }
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

            radioButton.buttonDrawable = appIconsList.values.elementAt(idx)
            radioButton.text = if (radioButton.buttonDrawable == null) context.getString(R.string.disabled) else ""
            radioButton.buttonTintList = null

            appIconSelector.addView(radioButton)
        }

        appIconSelector.check(selectedIdx)
    }

    private fun init() {
        fillAppIconsList()
    }

    private fun fillAppIconsList() {
        appIconsList[AppConstants.APP_ICON_NONE] = null
        appIconsList[AppConstants.APP_ICON_DEFAULT] = R.mipmap.ic_launcher.asDrawable(context)
        appIconsList[AppConstants.APP_ICON_ALT] = R.mipmap.ic_launcher_alt.asDrawable(context)
        appIconsList[AppConstants.APP_ICON_ALT_2] = R.mipmap.ic_launcher_alt_2.asDrawable(context)
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