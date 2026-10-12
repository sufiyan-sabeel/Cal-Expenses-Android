package com.umaiz.calexpenses.util

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("calexpenses_prefs", Context.MODE_PRIVATE)

    /** "system" | "light" | "dark" */
    var themeMode: String
        get() = sp.getString("theme_mode", "system") ?: "system"
        set(v) { sp.edit().putString("theme_mode", v).apply() }

    var currency: String
        get() = sp.getString("currency", "\u20B9") ?: "\u20B9"
        set(v) { sp.edit().putString("currency", v).apply() }
}
