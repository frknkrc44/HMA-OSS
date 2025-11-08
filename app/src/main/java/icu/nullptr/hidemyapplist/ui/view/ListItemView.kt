package icu.nullptr.hidemyapplist.ui.view

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.view.isVisible
import by.kirich1409.viewbindingdelegate.CreateMethod
import by.kirich1409.viewbindingdelegate.viewBinding
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.themeColor
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.ListItemViewBinding

class ListItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    val binding by viewBinding<ListItemViewBinding>(createMethod = CreateMethod.INFLATE)

    init {
        val typedArray = context.theme.obtainStyledAttributes(attrs, R.styleable.ListItemView, defStyleAttr, defStyleRes)
        val icon = typedArray.getResourceId(R.styleable.ListItemView_icon, 0)
        val text = typedArray.getString(R.styleable.ListItemView_text)
        val buttonText = typedArray.getText(R.styleable.ListItemView_buttonText)
        typedArray.recycle()
        binding.icon.setImageResource(icon)
        binding.text.text = text
        if (buttonText != null) {
            binding.button.visibility = VISIBLE
            binding.button.text = buttonText
        }
    }

    var text: CharSequence?
        get() = binding.text.text
        set(value) {
            binding.text.text = value
        }

    fun setIcon(@DrawableRes icon: Int) {
        binding.icon.setImageResource(icon)
    }

    fun showIcon(show: Boolean) {
        binding.icon.isVisible = show
    }

    fun showAsHeader() {
        showIcon(false)

        with(binding.text) {
            typeface = Typeface.DEFAULT_BOLD

            val textColor = context.themeColor(
                androidx.appcompat.R.attr.colorPrimary,
            )
            setTextColor(textColor)

            val padding = (resources.displayMetrics.density * 8f).toInt()
            setPaddingRelative(paddingStart, padding, paddingEnd, padding)
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {
        if (binding.button.isVisible) {
            binding.button.setOnClickListener(l)
        } else {
            super.setOnClickListener(l)
        }
    }
}
