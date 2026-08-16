package eu.ulonetwork.monitorapp.data

/**
 * SMTP configuration used by [eu.ulonetwork.monitorapp.mail.SmtpMailSender].
 * Persisted via encrypted shared preferences, never in plaintext.
 */
data class SmtpSettings(
    val host: String = "",
    val port: Int = 587,
    val username: String = "",
    val password: String = "",
    val fromAddress: String = "",
    val toAddress: String = "",
    val useTls: Boolean = true
) {
    fun isComplete(): Boolean {
        return host.isNotBlank() && username.isNotBlank() && password.isNotBlank() &&
            fromAddress.isNotBlank() && toAddress.isNotBlank() && port in 1..65535
    }
}
