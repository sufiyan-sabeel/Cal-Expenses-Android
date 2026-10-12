package com.umaiz.calexpenses

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.umaiz.calexpenses.databinding.ActivityMainBinding
import com.umaiz.calexpenses.ui.ai.AiChatFragment
import com.umaiz.calexpenses.ui.budgets.BudgetsFragment
import com.umaiz.calexpenses.ui.dashboard.DashboardFragment
import com.umaiz.calexpenses.ui.goals.GoalsFragment
import com.umaiz.calexpenses.ui.notes.NotesFragment
import com.umaiz.calexpenses.ui.reports.ReportsFragment
import com.umaiz.calexpenses.ui.settings.SettingsFragment
import com.umaiz.calexpenses.ui.tasks.TasksFragment
import com.umaiz.calexpenses.ui.transactions.TransactionsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> DashboardFragment()
                R.id.nav_transactions -> TransactionsFragment()
                R.id.nav_budgets -> BudgetsFragment()
                R.id.nav_goals -> GoalsFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> return@setOnItemSelectedListener false
            }
            show(fragment)
            true
        }

        if (savedInstanceState == null) binding.bottomNav.selectedItemId = R.id.nav_home

        requestNotificationPermission()
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_ai -> { show(AiChatFragment()); true }
            R.id.action_notes -> { show(NotesFragment()); true }
            R.id.action_tasks -> { show(TasksFragment()); true }
            R.id.action_reports -> { show(ReportsFragment()); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101
                )
            }
        }
    }
}
