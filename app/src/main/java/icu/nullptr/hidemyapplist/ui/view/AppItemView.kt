package top.secret.hma.v1.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.isVisible
import dev.androidbroadcast.vbpd.CreateMethod
import dev.androidbroadcast.vbpd.viewBinding
import top.secret.hma.v1.util.PackageHelper
import top.secret.hma.v1.databinding.AppItemViewBinding

class AppItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    val binding by viewBinding<AppItemViewBinding>(createMethod = CreateMethod.INFLATE)

    var showEnabled: Boolean
        get() = binding.enabled.isVisible
        set(value) {
            binding.enabled.visibility = if (value) VISIBLE else GONE
        }

    var isChecked: Boolean
        get() = binding.checkbox.isChecked
        set(value) {
            binding.checkbox.isChecked = value
        }

    constructor(context: Context, isCheckable: Boolean) : this(context) {
        binding.checkbox.visibility = if (isCheckable) VISIBLE else GONE
    }

    fun load(packageName: String) {
        binding.packageName.text = packageName
        try {
            binding.label.text = PackageHelper.loadAppLabel(packageName)
            binding.icon.setImageDrawable(PackageHelper.loadAppIcon(packageName))
        } catch (_: Throwable) {
            binding.label.text = packageName
            binding.icon.setImageResource(android.R.drawable.sym_def_app_icon)
        }
    }
}
