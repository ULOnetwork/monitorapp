package eu.ulonetwork.monitorapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import eu.ulonetwork.monitorapp.util.NotificationHelper

class UnetworkMonitorApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        applyDefaultLocaleIfUnset()
    }

    /**
     * Ensures English is used as the app's language until the user explicitly picks one via the
     * language picker in Settings, regardless of the device's system language. Once a locale has
     * been set (either this default, or an explicit user choice), [AppCompatDelegate] persists it
     * (via the platform LocaleManager on API 33+, or the appcompat back-compat store on API
     * 26-32), so this only actually changes anything on the very first launch.
     */
    private fun applyDefaultLocaleIfUnset() {
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
    }
}
