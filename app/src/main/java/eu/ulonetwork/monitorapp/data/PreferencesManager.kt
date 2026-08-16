package eu.ulonetwork.monitorapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages small app-level preferences: the one-time consent acknowledgement (plain
 * SharedPreferences, no sensitive data) and the SMTP settings (EncryptedSharedPreferences,
 * since these contain credentials).
 */
class PreferencesManager(context: Context) {

    private val appContext = context.applicationContext

    private val plainPrefs: SharedPreferences =
        appContext.getSharedPreferences(PLAIN_PREFS_NAME, Context.MODE_PRIVATE)

    private val encryptedPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            appContext,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // --- Consent ---

    fun hasAcceptedConsent(): Boolean = plainPrefs.getBoolean(KEY_CONSENT_ACCEPTED, false)

    fun setConsentAccepted() {
        plainPrefs.edit().putBoolean(KEY_CONSENT_ACCEPTED, true).apply()
    }

    // --- SMTP settings ---

    fun getSmtpSettings(): SmtpSettings {
        return SmtpSettings(
            host = encryptedPrefs.getString(KEY_SMTP_HOST, "") ?: "",
            port = encryptedPrefs.getInt(KEY_SMTP_PORT, 587),
            username = encryptedPrefs.getString(KEY_SMTP_USERNAME, "") ?: "",
            password = encryptedPrefs.getString(KEY_SMTP_PASSWORD, "") ?: "",
            fromAddress = encryptedPrefs.getString(KEY_SMTP_FROM, "") ?: "",
            toAddress = encryptedPrefs.getString(KEY_SMTP_TO, "") ?: "",
            useTls = encryptedPrefs.getBoolean(KEY_SMTP_USE_TLS, true)
        )
    }

    fun saveSmtpSettings(settings: SmtpSettings) {
        encryptedPrefs.edit()
            .putString(KEY_SMTP_HOST, settings.host)
            .putInt(KEY_SMTP_PORT, settings.port)
            .putString(KEY_SMTP_USERNAME, settings.username)
            .putString(KEY_SMTP_PASSWORD, settings.password)
            .putString(KEY_SMTP_FROM, settings.fromAddress)
            .putString(KEY_SMTP_TO, settings.toAddress)
            .putBoolean(KEY_SMTP_USE_TLS, settings.useTls)
            .apply()
    }

    companion object {
        private const val PLAIN_PREFS_NAME = "unetworkmonitor_prefs"
        private const val ENCRYPTED_PREFS_NAME = "unetworkmonitor_secure_prefs"

        private const val KEY_CONSENT_ACCEPTED = "consent_accepted"

        private const val KEY_SMTP_HOST = "smtp_host"
        private const val KEY_SMTP_PORT = "smtp_port"
        private const val KEY_SMTP_USERNAME = "smtp_username"
        private const val KEY_SMTP_PASSWORD = "smtp_password"
        private const val KEY_SMTP_FROM = "smtp_from"
        private const val KEY_SMTP_TO = "smtp_to"
        private const val KEY_SMTP_USE_TLS = "smtp_use_tls"
    }
}
