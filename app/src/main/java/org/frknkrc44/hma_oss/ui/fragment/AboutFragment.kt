package org.frknkrc44.hma_oss.ui.fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.bumptech.glide.Glide
import icu.nullptr.hidemyapplist.ui.util.ThemeUtils.homeItemBackgroundColor
import icu.nullptr.hidemyapplist.ui.util.navController
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.common.BuildConfig
import org.frknkrc44.hma_oss.databinding.FragmentAboutBinding
import org.frknkrc44.hma_oss.databinding.FragmentAboutListItemBinding
import org.json.JSONObject

@Suppress("deprecation")
class AboutFragment : Fragment(R.layout.fragment_about) {
    private val binding by viewBinding<FragmentAboutBinding>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val barInsets = insets.getInsets(WindowInsets.Type.systemBars())
                binding.root.setPadding(
                    barInsets.left,
                    barInsets.top,
                    barInsets.right,
                    0,
                )

                binding.bottomPadding.minimumHeight = barInsets.bottom
            } else {
                @Suppress("deprecation")
                binding.root.setPadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    0,
                )

                binding.bottomPadding.minimumHeight = insets.systemWindowInsetBottom
            }

            insets
        }
    }

    override fun onStart() {
        super.onStart()

        val tint = ColorStateList.valueOf(homeItemBackgroundColor())

        with(binding.aboutHeader) {
            with(backButton.parent as View) {
                setOnClickListener { navController.navigateUp()  }
                backgroundTintList = tint
            }

            Glide.with(this@AboutFragment)
                .load(R.mipmap.ic_launcher_round)
                .circleCrop()
                .into(appIcon)

            appName.setText(R.string.app_name)
            appVersion.text = BuildConfig.APP_VERSION_NAME

            setOnClickUrl(linkGithub, "https://github.com/frknkrc44/HMA-OSS")
            setOnClickUrl(linkTelegram, "https://t.me/aerathfuns")

            appInfoTop.backgroundTintList = tint
            (appName.parent as View).backgroundTintList = tint
        }

        with(binding.aboutDescription) {
            contentTitle.setText(R.string.title_about)
            contentDescription.setText(R.string.about_description)
            contentDescription.backgroundTintList = tint
        }

        with(binding.aboutForkDescription) {
            contentTitle.setText(R.string.title_about_fork)
            contentDescription.setText(R.string.about_fork_description)
            contentDescription.backgroundTintList = tint
        }

        with(binding.listDeveloper) {
            backgroundTintList = tint
            clipToOutline = true

            // HMA-OSS devs
            addDevItem(this, R.drawable.cont_fk, "frknkrc44", "HMA-OSS Developer", "https://github.com/frknkrc44")

            // Original HMA devs
            addDevItem(this, R.drawable.cont_nullptr, "nullptr", "HMA Developer", "https://github.com/Dr-TSNG")
            addDevItem(this, R.drawable.cont_k, "Ketal", "HMA Collaborator", "https://github.com/keta1")
            addDevItem(this, R.drawable.cont_aviraxp, "aviraxp", "HMA Collaborator", "https://github.com/aviraxp")
            addDevItem(this, R.drawable.cont_icon_designer, "辉少菌", "HMA Icon Designer", "http://www.coolapk.com/u/1560270")
            addDevItem(this, R.drawable.cont_cpp_master,  "LoveSy", "HMA Idea Provider", "https://github.com/yujincheng08")
        }

        with(binding.listTranslator) {
            backgroundTintList = tint
            clipToOutline = true

            val jsonObj = JSONObject(String(requireContext().assets.open("translators.json").readBytes()))
            val jsonKeys = jsonObj.keys().asSequence().sortedWith { a, b -> a.lowercase().compareTo(b.lowercase()) }
            for (name in jsonKeys) {
                val avatarUrl = jsonObj.getString(name)
                addTranslatorItem(this, avatarUrl, name)
            }
        }
    }

    fun addDevItem(layout: LinearLayout, @DrawableRes avatarResId: Int, name: String, desc: String, url: String) {
        val newLayout = FragmentAboutListItemBinding.inflate(layoutInflater)
        setOnClickUrl(newLayout.root, url)

        Glide.with(this)
            .load(avatarResId)
            .circleCrop()
            .into(newLayout.aboutPersonIcon)

        newLayout.text1.text = name
        newLayout.text2.text = desc
        layout.addView(newLayout.root)
    }

    fun addTranslatorItem(layout: LinearLayout, avatarUrl: String, name: String) {
        val newLayout = FragmentAboutListItemBinding.inflate(layoutInflater)
        Glide.with(this)
            .load(avatarUrl)
            .placeholder(R.drawable.outline_info_24)
            .circleCrop()
            .into(newLayout.aboutPersonIcon)

        newLayout.text1.text = name
        newLayout.text2.visibility = View.GONE
        layout.addView(newLayout.root)
    }

    fun setOnClickUrl(view: View, url: String) {
        view.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setData(url.toUri())
            startActivity(intent)
        }
    }
}