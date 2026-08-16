package eu.ulonetwork.monitorapp.data

import android.util.Base64
import org.json.JSONObject

/**
 * Encodes/decodes [MailjetSettings] as a compact, copy-pasteable text blob so the same e-mail
 * configuration can be transferred between the owner's own devices without re-typing the API Key
 * and Secret Key by hand.
 *
 * The blob is base64-encoded JSON behind a small versioned prefix. This is NOT encryption, just
 * a compact transport format — an exported code contains the API Key and Secret Key in trivially
 * decodable form and must be handled like the credentials themselves (e.g. not pasted into chat
 * apps or notes synced to third parties).
 */
object MailjetSettingsCodec {
    private const val PREFIX = "UMJMAIL1:"

    fun encode(settings: MailjetSettings): String {
        val json = JSONObject().apply {
            put("apiKey", settings.apiKey)
            put("secretKey", settings.secretKey)
            put("fromAddress", settings.fromAddress)
            put("fromName", settings.fromName)
            put("toAddress", settings.toAddress)
        }
        val encoded = Base64.encodeToString(json.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return PREFIX + encoded
    }

    /** Returns the decoded settings, or null if [text] isn't a recognizable export blob. */
    fun decode(text: String): MailjetSettings? {
        val trimmed = text.trim()
        if (!trimmed.startsWith(PREFIX)) return null
        return try {
            val jsonBytes = Base64.decode(trimmed.removePrefix(PREFIX), Base64.NO_WRAP)
            val json = JSONObject(String(jsonBytes, Charsets.UTF_8))
            val settings = MailjetSettings(
                apiKey = json.optString("apiKey", ""),
                secretKey = json.optString("secretKey", ""),
                fromAddress = json.optString("fromAddress", ""),
                fromName = json.optString("fromName", ""),
                toAddress = json.optString("toAddress", "")
            )
            settings.takeIf { it.apiKey.isNotBlank() || it.secretKey.isNotBlank() || it.fromAddress.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}
