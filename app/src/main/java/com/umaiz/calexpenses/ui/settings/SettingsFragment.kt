package com.umaiz.calexpenses.ui.settings

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umaiz.calexpenses.CalExpensesApp
import com.umaiz.calexpenses.MainActivity
import com.umaiz.calexpenses.R
import com.umaiz.calexpenses.databinding.FragmentSettingsBinding
import com.umaiz.calexpenses.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private var _b: FragmentSettingsBinding? = null
    private val b get() = _b!!
    private val app get() = requireActivity().application as CalExpensesApp

    private val createJson = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { doExportJson(it) } }

    private val createCsv = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { doExportCsv(it) } }

    private val openJson = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { doImport(it) } }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragmentSettingsBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.supportActionBar?.title = getString(R.string.title_settings)

        // ---- appearance: system / light / dark ----
        val themeLabels = listOf("System default", "Light", "Dark")
        val themeValues = listOf("system", "light", "dark")
        b.spTheme.adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, themeLabels)
        b.spTheme.setSelection(themeValues.indexOf(app.prefs.themeMode).coerceAtLeast(0))
        b.spTheme.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val value = themeValues[position]
                if (value == app.prefs.themeMode) return
                app.prefs.themeMode = value
                AppCompatDelegate.setDefaultNightMode(
                    when (value) {
                        "light" -> AppCompatDelegate.MODE_NIGHT_NO
                        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                )
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // ---- currency ----
        val symbols = listOf("\u20B9", "\u0024", "\u20AC", "\u00A3")
        b.spCurrency.adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, symbols)
        b.spCurrency.setSelection(symbols.indexOf(app.prefs.currency).coerceAtLeast(0))
        b.spCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val sym = symbols[position]
                app.prefs.currency = sym
                Formatters.currencySymbol = sym
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        b.btnExportJson.setOnClickListener { createJson.launch("calexpenses-backup.json") }
        b.btnExportCsv.setOnClickListener { createCsv.launch("calexpenses-expenses.csv") }
        b.btnImportJson.setOnClickListener {
            openJson.launch(arrayOf("application/json", "text/plain", "*/*"))
        }

        b.btnClearChats.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Clear Cal AI chat history?")
                .setMessage("This deletes all saved conversations and messages on this device.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton("Clear") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        app.repository.clearAllChats()
                        toast("Chat history cleared")
                    }
                }
                .show()
        }

        b.tvAbout.text = "Cal-Expenses v1.3\n" +
            "A privacy-first personal finance and voice productivity application.\n" +
            "Created by Umaiz Sufiyan.\n\n" +
            "No accounts, no ads, no tracking. All financial data stays on this device.\n\n" +
            "AI disclosure: Cal AI sends your typed message (and, only if you enable the " +
            "spending-summary switch, a short summary of your totals) to OpenRouter to generate " +
            "a reply. Your API key is stored encrypted on this device. Every other feature works " +
            "fully offline without any account or key."
    }

    private fun doExportJson(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { app.backupManager.exportJson(uri) }
                toast("Backup saved")
            } catch (e: Exception) {
                toast("Export failed: ${e.message}")
            }
        }
    }

    private fun doExportCsv(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) { app.backupManager.exportCsv(uri) }
                toast("CSV saved")
            } catch (e: Exception) {
                toast("Export failed: ${e.message}")
            }
        }
    }

    private fun doImport(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            val parsed = try {
                withContext(Dispatchers.IO) { app.backupManager.parseBackup(uri) }
            } catch (e: Exception) {
                toast(e.message ?: "Invalid backup file")
                return@launch
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Replace all data?")
                .setMessage(
                    "The backup contains ${parsed.totalCount} items " +
                        "(${parsed.expenses.size} expenses, ${parsed.budgets.size} budgets, " +
                        "${parsed.notes.size} notes, ${parsed.tasks.size} tasks).\n\n" +
                        "Importing replaces your current data. Export a backup first if unsure."
                )
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton("Import") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            withContext(Dispatchers.IO) { app.backupManager.applyImport(parsed) }
                            toast("Import complete")
                        } catch (e: Exception) {
                            toast("Import failed: ${e.message}")
                        }
                    }
                }
                .show()
        }
    }

    private fun toast(m: String) = Toast.makeText(requireContext(), m, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
