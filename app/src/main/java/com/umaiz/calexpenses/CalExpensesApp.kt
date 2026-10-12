package com.umaiz.calexpenses

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.umaiz.calexpenses.data.AppDatabase
import com.umaiz.calexpenses.data.BackupManager
import com.umaiz.calexpenses.data.Repository
import com.umaiz.calexpenses.util.Formatters
import com.umaiz.calexpenses.util.Notifications
import com.umaiz.calexpenses.util.Prefs

class CalExpensesApp : Application() {

    lateinit var repository: Repository
        private set
    lateinit var backupManager: BackupManager
        private set
    lateinit var prefs: Prefs
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        AppCompatDelegate.setDefaultNightMode(
            when (prefs.themeMode) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
        Formatters.currencySymbol = prefs.currency
        val db = AppDatabase.get(this)
        repository = Repository(db)
        backupManager = BackupManager(this, repository)
        Notifications.createChannel(this)
    }
}
