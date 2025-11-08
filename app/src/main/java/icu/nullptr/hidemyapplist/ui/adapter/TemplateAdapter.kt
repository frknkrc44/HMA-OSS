package icu.nullptr.hidemyapplist.ui.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import icu.nullptr.hidemyapplist.service.ConfigManager
import icu.nullptr.hidemyapplist.ui.view.ListItemView
import org.frknkrc44.hma_oss.R
import java.text.Collator
import java.util.Locale

class TemplateAdapter(
    private val onClickListener: ((ConfigManager.TemplateInfo) -> Unit)?
) : RecyclerView.Adapter<TemplateAdapter.ViewHolder>() {

    private lateinit var list: List<ConfigManager.TemplateInfo>

    init {
        updateList()
    }

    inner class ViewHolder(view: ListItemView) : RecyclerView.ViewHolder(view) {
        init {
            view.setOnClickListener {
                onClickListener?.invoke(list[absoluteAdapterPosition])
            }
        }

        fun bind(info: ConfigManager.TemplateInfo) {
            with(itemView as ListItemView) {
                setIcon(
                    when (info.type) {
                        ConfigManager.PTType.APP -> {
                            if (info.isWhiteList) R.drawable.outline_assignment_24
                            else R.drawable.baseline_assignment_24
                        }
                        ConfigManager.PTType.SETTINGS -> R.drawable.baseline_settings_24
                    }
                )
                text = info.name
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = ListItemView(parent.context)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun getItemId(position: Int) = list[position].name.hashCode().toLong()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(list[position])

    @SuppressLint("NotifyDataSetChanged")
    fun updateList() {
        val allItems = (ConfigManager.getTemplateList() + ConfigManager.getSettingTemplateList()).toMutableList()

        list = allItems.apply {
            sortWith { o1, o2 ->
                if (o1.type != o2.type) {
                    o1.type.compareTo(o2.type)
                } else if (o1.isWhiteList != o2.isWhiteList) {
                    o1.isWhiteList.compareTo(o2.isWhiteList)
                } else {
                    Collator.getInstance(Locale.getDefault()).compare(o1.name, o2.name)
                }
            }
        }
        notifyDataSetChanged()
    }
}
