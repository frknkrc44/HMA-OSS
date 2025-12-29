package icu.nullptr.hidemyapplist.ui.util

import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.ui.activity.MainActivity

val Fragment.navController get() = NavHostFragment.findNavController(this)

private val navOptions by lazy {
    NavOptions.Builder().apply {
        setEnterAnim(R.anim.activity_open_enter)
        setExitAnim(R.anim.activity_open_exit)
        setPopEnterAnim(R.anim.activity_close_enter)
        setPopExitAnim(R.anim.activity_close_exit)
    }.build()
}

fun Fragment.navigate(@IdRes resId: Int, args: Bundle? = null) {
    navController.navigate(resId, args, navOptions)
}

fun Fragment.setupToolbar(
    toolbar: Toolbar,
    title: String,
    @DrawableRes navigationIcon: Int? = null,
    navigationOnClick: View.OnClickListener? = null,
    @MenuRes menuRes: Int? = null,
    onMenuOptionSelected: ((MenuItem) -> Unit)? = null
) {
    navigationOnClick?.let { toolbar.setNavigationOnClickListener(it) }
    navigationIcon?.let { toolbar.setNavigationIcon(navigationIcon) }
    toolbar.title = title
    toolbar.tooltipText = title
    if (menuRes != null) {
        val menuProvider = object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(menuRes, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return onMenuOptionSelected?.let {
                    it(menuItem); true
                } ?: false
            }
        }
        toolbar.inflateMenu(menuRes)
        toolbar.setOnMenuItemClickListener(menuProvider::onMenuItemSelected)
        requireActivity().addMenuProvider(menuProvider)
        menuProvider.onPrepareMenu(toolbar.menu)
    }
}

val Fragment.contentResolver get(): ContentResolver = requireContext().contentResolver

fun Fragment.recreateMainActivity(restart: Boolean = false) {
    val mainActivity = activity as MainActivity
    mainActivity.readyToKill = false

    if (restart) {
        mainActivity.finish()
        startActivity(Intent(mainActivity, mainActivity.javaClass))
    } else {
        mainActivity.recreate()
    }
}

fun FragmentTransaction.withAnimations() = setCustomAnimations(
        R.anim.activity_open_enter,
        R.anim.activity_open_exit,
        R.anim.activity_close_enter,
        R.anim.activity_close_exit,
    )
