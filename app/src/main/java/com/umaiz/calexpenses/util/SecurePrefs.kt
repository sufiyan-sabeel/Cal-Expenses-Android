package com.umaiz.calexpenses.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Stores the OpenRouter API key encrypted at rest, backed by the Android Keystore.
 * The key is never written to source control, build files or the APK.
 *
 * Returns null (rather than crashing) if the Keystore is unavailable, so the AI
 * features can degrade gracefully while the rest of the app keeps working.
 */
object SecurePrefs {

    private const val FILE = "cal_secure_prefs"
    private const val KEY_API = "openrouter_api_key"

    @Volatile private var cached: SharedPreferences? = null

    private fun prefs(context: Context): SharedPreferences? {
        cached?.let { return it }
        return try {
            val alias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                FILE, alias, context.applicationContext,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also { cached = it }
        } catch (e: Exception) {
            null
        }
    }

    fun apiKey(context: Context): String =
        prefs(context)?.getString(KEY_API, "").orEmpty()

    fun hasKey(context: Context): Boolean = apiKey(context).isNotBlank()

    fun setApiKey(context: Context, value: String) {
        prefs(context)?.edit()?.putString(KEY_API, value.trim())?.apply()
    }

    fun clearApiKey(context: Context) {
        prefs(context)?.edit()?.remove(KEY_API)?.apply()
    }
}
