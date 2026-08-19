package eu.ulonetwork.monitorapp.data

import android.util.Base64
import org.json.JSONObject

/**
 * Encodes/decodes [TelegramSettings] as a compact, copy-pasteable text blob, the same way
 * [MailjetSettingsCodec] does for the Mailjet settings, so the same Telegram configuration can be
 * transferred between the owner's own devices without re-typing the bot token by hand.
 *
 * The blob is base64-encoded JSON behind a small versioned prefix. This is NOT encryption, just a
 * compact transport format — an exported code contains the bot token in trivially decodable form
 * and must be handled like a credential (e.g. not pasted into chat apps or notes synced to third
 * parties).
 */
object TelegramSettingsCodec {
    private const val PREFIX = "UMTG1:"

    fun encode(settings: TelegramSettings): String {
        val json = JSONObject().apply {
            put("botToken", settings.botToken)
            put("chatId", settings.chatId)
        }
        val encoded = Base64.encodeToString(json.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return PREFIX + encoded
    }

    /** Returns the decoded settings, or null if [text] isn't a recognizable export blob. */
    fun decode(text: String): TelegramSettings? {
        val trimmed = text.trim()
        if (!trimmed.startsWith(PREFIX)) return null
        return try {
            val jsonBytes = Base64.decode(trimmed.removePrefix(PREFIX), Base64.NO_WRAP)
            val json = JSONObject(String(jsonBytes, Charsets.UTF_8))
            val settings = TelegramSettings(
                botToken = json.optString("botToken", ""),
                chatId = json.optString("chatId", "")
            )
            settings.takeIf { it.botToken.isNotBlank() || it.chatId.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}
