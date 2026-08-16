package eu.ulonetwork.monitorapp.data

/**
 * Mailjet Send API v3.1 configuration used by [eu.ulonetwork.monitorapp.mail.MailjetMailSender].
 * Persisted via encrypted shared preferences, never in plaintext.
 *
 * Mail is sent over HTTPS (port 443) via Mailjet's REST API rather than raw SMTP, since SMTP
 * ports (25/465/587) are commonly blocked by network firewalls even when HTTPS traffic is not.
 */
data class MailjetSettings(
    val apiKey: String = "",
    val secretKey: String = "",
    val fromAddress: String = "",
    val fromName: String = "",
    val toAddress: String = ""
) {
    fun isComplete(): Boolean {
        return apiKey.isNotBlank() && secretKey.isNotBlank() &&
            fromAddress.isNotBlank() && toAddress.isNotBlank()
    }
}
