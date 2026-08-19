package eu.ulonetwork.monitorapp.data

/**
 * Telegram Bot API configuration used by [eu.ulonetwork.monitorapp.telegram.TelegramSender].
 * Persisted via encrypted shared preferences, never in plaintext, since the bot token is a
 * credential (anyone holding it can send messages as the bot).
 */
data class TelegramSettings(
    val botToken: String = "",
    val chatId: String = ""
) {
    fun isComplete(): Boolean {
        return botToken.isNotBlank() && chatId.isNotBlank()
    }
}
