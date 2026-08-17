package eu.ulonetwork.monitorapp.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

/**
 * Best-effort device identification for alert e-mails. Modern Android restricts access to the
 * true hardware serial number to system/device-owner apps (Build.getSerial() throws
 * SecurityException for regular apps since API 29), so ANDROID_ID is used as a stable per-device
 * substitute identifier instead.
 */
object DeviceInfoProvider {

    fun deviceName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model".trim()
        }.ifBlank { "unknown" }
    }

    fun deviceIdentifier(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?.takeIf { it.isNotBlank() } ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    fun localIpAddress(): String {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue
                val addresses = Collections.list(iface.inetAddresses)
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        address.hostAddress?.let { return it }
                    }
                }
            }
            "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
}
