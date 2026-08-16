package eu.ulonetwork.monitorapp.util

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import eu.ulonetwork.monitorapp.service.ScreenReaderAccessibilityService

object AccessibilityUtils {

    /**
     * Checks whether [ScreenReaderAccessibilityService] is currently enabled for this app via
     * the system's accessibility settings. This cannot be granted programmatically; the user
     * must enable it manually in Settings > Toegankelijkheid.
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = "${context.packageName}/${ScreenReaderAccessibilityService::class.java.name}"

        val accessibilityEnabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        )
        if (accessibilityEnabled != 1) return false

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
