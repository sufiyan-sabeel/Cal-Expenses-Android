package com.umaiz.calexpenses.ui.ai

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.umaiz.calexpenses.databinding.SheetKeyBinding
import com.umaiz.calexpenses.util.SecurePrefs

/** Lets the user paste their own OpenRouter key, stored encrypted on-device. */
class KeyDialog : BottomSheetDialogFragment() {

    private var _b: SheetKeyBinding? = null
    private val b get() = _b!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = SheetKeyBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        b.etKey.setText(SecurePrefs.apiKey(requireContext()))

        b.btnSave.setOnClickListener {
            val key = b.etKey.text?.toString()?.trim().orEmpty()
            if (key.isBlank()) { toast("Enter your OpenRouter key"); return@setOnClickListener }
            if (!key.startsWith("sk-")) {
                toast("That doesn't look right — OpenRouter keys start with \"sk-\"")
                return@setOnClickListener
            }
            SecurePrefs.setApiKey(requireContext(), key)
            notifyChanged()
            dismiss()
        }

        b.btnClear.setOnClickListener {
            SecurePrefs.clearApiKey(requireContext())
            notifyChanged()
            dismiss()
        }
    }

    private fun notifyChanged() {
        parentFragmentManager.setFragmentResult(REQUEST_KEY, Bundle())
    }

    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() { super.onDestroyView(); _b = null }

    companion object { const val REQUEST_KEY = "ai_key_changed" }
}
