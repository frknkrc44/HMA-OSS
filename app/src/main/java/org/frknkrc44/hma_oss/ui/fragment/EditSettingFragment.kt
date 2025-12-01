package org.frknkrc44.hma_oss.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import icu.nullptr.hidemyapplist.common.Constants
import icu.nullptr.hidemyapplist.service.ServiceClient
import icu.nullptr.hidemyapplist.ui.util.get
import icu.nullptr.hidemyapplist.ui.util.navController
import icu.nullptr.hidemyapplist.ui.util.setupToolbar
import kotlinx.coroutines.flow.MutableSharedFlow
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.FragmentEditSettingBinding

class EditSettingFragment : Fragment(R.layout.fragment_edit_setting) {
    private val binding by viewBinding<FragmentEditSettingBinding>()

    private val args by navArgs<EditSettingFragmentArgs>()

    var settingsItems = MutableSharedFlow<Array<String>>(replay = 1)

    fun saveResult() {
        val databaseName = getEditTextValue(binding.databaseSelector)
        val settingName = getEditTextValue(binding.settingName)

        if (databaseName != null && settingName != null) {
            val settingValue = getEditTextValue(binding.settingValue, false)
            val isValueNull = binding.settingValueNull.isChecked

            setFragmentResult("edit_setting", Bundle().apply {
                putStringArray(
                    settingName,
                    arrayOf(
                        if (isValueNull) null else (settingValue ?: ""),
                        databaseName,
                    )
                )
            })
        }
    }

    fun onBack() {
        saveResult()

        navController.navigateUp()
    }

    fun getEditTextValue(textInputLayout: TextInputLayout, returnNullOnBlank: Boolean = true): String? {
        if (textInputLayout.editText?.text.isNullOrBlank()) {
            return if (returnNullOnBlank) null else ""
        }

        return textInputLayout.editText!!.text.toString()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { onBack() }

        setupToolbar(
            toolbar = binding.toolbar,
            title = getString(R.string.edit),
            navigationIcon = R.drawable.baseline_arrow_back_24,
            navigationOnClick = { onBack() },
        )

        with(binding.databaseSelector.editText as MaterialAutoCompleteTextView) {
            val values = arrayOf(Constants.SETTINGS_GLOBAL, Constants.SETTINGS_SECURE, Constants.SETTINGS_SYSTEM)
            setSimpleItems(values)
            setText(args.database)

            if (args.database != null) {
                fillSettingNames(args.database.toString())
            }

            setOnItemClickListener { _, _, _, _ ->
                fillSettingNames(text.toString())
            }
        }

        with(binding.settingsGroupShowAll) {
            setOnClickListener {
                if (settingsItems.replayCache.isEmpty()) return@setOnClickListener

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.settings_templates_name)
                    .setItems(settingsItems.get()) { _, which ->
                        binding.settingName.editText?.setText(settingsItems.get()[which])
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }

        with(binding.settingName.editText as MaterialAutoCompleteTextView) {
            setText(args.name)
        }

        with(binding.settingValueNull) {
            setOnCheckedChangeListener { _, isChecked ->
                binding.settingValue.isEnabled = !isChecked

                if (isChecked) binding.settingValue.editText?.setText("")
            }
        }

        with(binding.settingValue.editText as MaterialAutoCompleteTextView) {
            setText(args.value ?: "")
            binding.settingValueNull.isChecked = args.value == null && args.database != null
        }

        binding.root.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val barInsets = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(
                    barInsets.left,
                    barInsets.top,
                    barInsets.right,
                    barInsets.bottom,
                )
            } else {
                @Suppress("deprecation")
                v.setPadding(
                    insets.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom,
                )
            }

            insets
        }
    }

    fun fillSettingNames(databaseName: String) {
        with(binding.settingName) {
            isEnabled = true

            settingsItems.tryEmit(ServiceClient.listAllSettings(databaseName))

            (editText as MaterialAutoCompleteTextView).setSimpleItems(settingsItems.get())
        }
    }
}