package eu.ulonetwork.monitorapp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages small app-level preferences: the one-time consent acknowledgement (plain
 * SharedPreferences, no sensitive data) and the Mailjet settings (EncryptedSharedPreferences,
 * since the API Key / Secret Key are credentials).
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

    // --- Mailjet settings ---

    fun getMailjetSettings(): MailjetSettings {
        return MailjetSettings(
            apiKey = encryptedPrefs.getString(KEY_MAILJET_API_KEY, "") ?: "",
            secretKey = encryptedPrefs.getString(KEY_MAILJET_SECRET_KEY, "") ?: "",
            fromAddress = encryptedPrefs.getString(KEY_MAILJET_FROM_ADDRESS, "") ?: "",
            fromName = encryptedPrefs.getString(KEY_MAILJET_FROM_NAME, "") ?: "",
            toAddress = encryptedPrefs.getString(KEY_MAILJET_TO_ADDRESS, "") ?: ""
        )
    }

    fun saveMailjetSettings(settings: MailjetSettings) {
        encryptedPrefs.edit()
            .putString(KEY_MAILJET_API_KEY, settings.apiKey)
            .putString(KEY_MAILJET_SECRET_KEY, settings.secretKey)
            .putString(KEY_MAILJET_FROM_ADDRESS, settings.fromAddress)
            .putString(KEY_MAILJET_FROM_NAME, settings.fromName)
            .putString(KEY_MAILJET_TO_ADDRESS, settings.toAddress)
            .apply()
    }

    companion object {
        private const val PLAIN_PREFS_NAME = "unetworkmonitor_prefs"
        private const val ENCRYPTED_PREFS_NAME = "unetworkmonitor_secure_prefs"

        private const val KEY_CONSENT_ACCEPTED = "consent_accepted"

        private const val KEY_MAILJET_API_KEY = "mailjet_api_key"
        private const val KEY_MAILJET_SECRET_KEY = "mailjet_secret_key"
        private const val KEY_MAILJET_FROM_ADDRESS = "mailjet_from_address"
        private const val KEY_MAILJET_FROM_NAME = "mailjet_from_name"
        private const val KEY_MAILJET_TO_ADDRESS = "mailjet_to_address"
    }
}
