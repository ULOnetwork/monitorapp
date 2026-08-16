package eu.ulonetwork.monitorapp.mail

import android.util.Log
import eu.ulonetwork.monitorapp.data.SmtpSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

/**
 * Sends alert e-mails over a user-configured SMTP server using JavaMail (android-mail /
 * android-activation). All network I/O runs on [Dispatchers.IO].
 *
 * Connection security is chosen based on both the "use TLS" toggle and the configured port:
 * - Port 465 with TLS on: implicit SSL (the socket is encrypted from the very first byte).
 * - Any other port with TLS on (587/25/etc.): STARTTLS (plaintext connect, then upgrade).
 * - TLS off: no encryption properties are set (only appropriate for local/test relays).
 *
 * Mixing these up (e.g. sending STARTTLS commands to a port that expects implicit SSL, or vice
 * versa) is a common cause of a generic "Exception reading response" MessagingException, since
 * the server and client end up disagreeing about the wire protocol from the first packet.
 */
class SmtpMailSender {

    sealed class Result {
        data object Success : Result()
        data class Failure(val message: String) : Result()
    }

    suspend fun send(settings: SmtpSettings, subject: String, body: String): Result =
        withContext(Dispatchers.IO) {
            if (!settings.isComplete()) {
                return@withContext Result.Failure("SMTP settings are incomplete")
            }
            try {
                val props = buildSessionProperties(settings)

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(settings.username, settings.password)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(settings.fromAddress))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(settings.toAddress))
                    setSubject(subject)
                    setText(body)
                }

                Transport.send(message)
                Result.Success
            } catch (e: Exception) {
                val diagnostic = describeChain(e)
                Log.e(TAG, "Failed to send e-mail: $diagnostic", e)
                Result.Failure(diagnostic)
            }
        }

    private fun buildSessionProperties(settings: SmtpSettings): Properties {
        return Properties().apply {
            put("mail.smtp.host", settings.host)
            put("mail.smtp.port", settings.port.toString())
            put("mail.smtp.auth", "true")

            when {
                settings.useTls && settings.port == IMPLICIT_SSL_PORT -> {
                    // Implicit SSL (e.g. port 465): the socket must be TLS-wrapped from the
                    // first byte, STARTTLS properties must NOT be set here.
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.socketFactory.fallback", "false")
                    put("mail.smtp.socketFactory.port", settings.port.toString())
                    put("mail.smtp.ssl.enable", "true")
                    put("mail.smtp.ssl.trust", settings.host)
                    put("mail.smtp.ssl.protocols", "TLSv1.2")
                }
                settings.useTls -> {
                    // STARTTLS (e.g. ports 587/25): connect in plaintext, then upgrade.
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.starttls.required", "true")
                    put("mail.smtp.ssl.trust", settings.host)
                    put("mail.smtp.ssl.protocols", "TLSv1.2")
                }
                else -> {
                    // TLS explicitly disabled by the user; no encryption properties are set.
                    // Only appropriate for local/test SMTP relays.
                }
            }

            put("mail.smtp.connectiontimeout", CONNECT_TIMEOUT_MS)
            put("mail.smtp.timeout", READ_TIMEOUT_MS)
            put("mail.smtp.writetimeout", WRITE_TIMEOUT_MS)
        }
    }

    /**
     * The top-level `MessagingException` message alone (e.g. "Exception reading response") is
     * usually just a generic wrapper — the actually useful diagnostic (SSL handshake failure,
     * connection reset, auth rejected, unknown host, ...) is in the wrapped/nested exception.
     * Walk both the standard [Throwable.cause] chain and JavaMail's own
     * [MessagingException.getNextException] chaining (older JavaMail versions don't always mirror
     * one into the other) and report every distinct exception in the chain.
     */
    private fun describeChain(root: Throwable): String {
        val parts = mutableListOf<String>()
        val seen = mutableSetOf<Throwable>()
        var current: Throwable? = root
        while (current != null && seen.add(current)) {
            val throwable = current
            parts += "${throwable.javaClass.simpleName}: ${throwable.message ?: "no further details"}"
            val next = (throwable as? MessagingException)?.nextException?.takeIf { it !== throwable.cause }
            current = next ?: throwable.cause
        }
        return parts.joinToString(separator = " -> caused by ")
    }

    companion object {
        private const val TAG = "SmtpMailSender"
        private const val IMPLICIT_SSL_PORT = 465
        private const val CONNECT_TIMEOUT_MS = "15000"
        private const val READ_TIMEOUT_MS = "15000"
        private const val WRITE_TIMEOUT_MS = "15000"
    }
}
