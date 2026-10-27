package com.unrot.app.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LockPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("unrot_lock", Context.MODE_PRIVATE)

    fun hasPin(): Boolean = !prefs.getString(KEY_HASH, null).isNullOrBlank()

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC, false)

    fun setPin(pin: String) {
        val salt = PinHasher.generateSalt()
        prefs.edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, PinHasher.hash(pin, salt))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val salt = prefs.getString(KEY_SALT, "") ?: return false
        val hash = prefs.getString(KEY_HASH, "") ?: return false
        return PinHasher.matches(pin, salt, hash)
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_SALT)
            .remove(KEY_HASH)
            .putBoolean(KEY_BIOMETRIC, false)
            .apply()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, enabled).apply()
    }

    companion object {
        private const val KEY_SALT = "pin_salt"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_BIOMETRIC = "biometric_enabled"
    }
}
