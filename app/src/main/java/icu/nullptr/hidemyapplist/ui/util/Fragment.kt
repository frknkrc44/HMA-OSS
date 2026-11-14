package icu.nullptr.hidemyapplist.ui.util

import android.content.ContentResolver
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.MenuRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.doOnAttach
import androidx.core.view.updatePadding
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
    val cr = requireContext().contentResolver
    if (AccessibilityUtils.isAnimationEnabled(cr)) {
        navController.navigate(resId, args, navOptions)
    } else {
        navController.navigate(resId, args)
    }
}

fun Fragment.setupToolbar(
    toolbar: Toolbar,
    title: String,
    subtitle: String? = null,
    @DrawableRes navigationIcon: Int? = null,
    navigationOnClick: View.OnClickListener? = null,
    @MenuRes menuRes: Int? = null,
    onMenuOptionSelected: ((MenuItem) -> Unit)? = null,
) {
    navigationOnClick?.let { toolbar.setNavigationOnClickListener(it) }
    navigationIcon?.let { toolbar.setNavigationIcon(navigationIcon) }
    toolbar.title = title
    if (subtitle != null) toolbar.subtitle = subtitle
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
        toolbar.menu.clear()
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

private val safeDrawingInsets =
    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()

fun ViewGroup.applyPageWindowInsets(bottomContent: View? = this) {
    // Inset the contents of the app bar so its background still reaches the window edges.
    val childPadding = children.map { Triple(it, it.paddingLeft, it.paddingRight) }.toList()
    val initialBottom = paddingBottom
    if (bottomContent != null && bottomContent !== this) {
        bottomContent.applyBottomWindowInsets()
    }

    ViewCompat.setOnApplyWindowInsetsListener(this) { page, insets ->
        val sides = insets.getInsets(safeDrawingInsets)
        for ((child, left, right) in childPadding) {
            child.updatePadding(left = left + sides.left, right = right + sides.right)
        }
        if (bottomContent === this) {
            page.updatePadding(bottom = initialBottom + insets.contentBottomInset())
        }
        insets
    }
    doOnAttach { ViewCompat.requestApplyInsets(it) }
}

fun View.applyBottomWindowInsets() {
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        view.updatePadding(bottom = initialBottom + insets.contentBottomInset())
        insets
    }
    doOnAttach { ViewCompat.requestApplyInsets(it) }
}

private fun WindowInsetsCompat.contentBottomInset() =
    getInsets(safeDrawingInsets or WindowInsetsCompat.Type.ime()).bottom

fun Fragment.registerOnBackCallback(back: () -> Unit) =
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { back() }
