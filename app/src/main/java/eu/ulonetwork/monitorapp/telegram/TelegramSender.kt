package eu.ulonetwork.monitorapp.telegram

import android.util.Log
import eu.ulonetwork.monitorapp.data.TelegramSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

/**
 * Sends alert messages to a Telegram chat via the Bot API's `sendMessage` method
 * (https://api.telegram.org/bot<token>/sendMessage) over plain HTTPS, using
 * [HttpsURLConnection] and [org.json] only — same approach as [eu.ulonetwork.monitorapp.mail.MailjetMailSender],
 * no extra HTTP client dependency.
 *
 * The message is sent without a `parse_mode`, i.e. as plain text: on-screen text snippets can
 * contain characters that are meaningful in Telegram's Markdown/HTML modes (e.g. `*`, `_`, `<`),
 * and a parse error there would silently break delivery of the alert itself.
 *
 * All network I/O runs on [Dispatchers.IO].
 */
class TelegramSender {

    sealed class Result {
        data object Success : Result()
        data class Failure(val message: String) : Result()
    }

    suspend fun send(settings: TelegramSettings, text: String): Result =
        withContext(Dispatchers.IO) {
            if (!settings.isComplete()) {
                return@withContext Result.Failure("Telegram settings are incomplete")
            }

            var connection: HttpsURLConnection? = null
            try {
                val url = URL(SEND_ENDPOINT_TEMPLATE.format(settings.botToken))
                connection = (url.openConnection() as HttpsURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }

                val requestBody = JSONObject().apply {
                    put("chat_id", settings.chatId)
                    put("text", text)
                }.toString()
                connection.outputStream.use { it.write(requestBody.toByteArray(StandardCharsets.UTF_8)) }

                val statusCode = connection.responseCode
                val responseStream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
                val responseText = readStream(responseStream)

                parseResult(statusCode, responseText)
            } catch (e: Exception) {
                val diagnostic = "${e.javaClass.simpleName}: ${e.message ?: "no further details"}"
                Log.e(TAG, "Failed to send Telegram message: $diagnostic", e)
                Result.Failure(diagnostic)
            } finally {
                connection?.disconnect()
            }
        }

    private fun readStream(stream: InputStream?): String {
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
    }

    /**
     * Telegram always responds with a JSON body carrying `"ok": true/false`, including for
     * non-2xx HTTP statuses (e.g. 400 for an invalid chat_id, 401 for a bad token), with the
     * human-readable reason in `"description"`.
     */
    private fun parseResult(statusCode: Int, responseText: String): Result {
        val json = try {
            if (responseText.isNotBlank()) JSONObject(responseText) else null
        } catch (e: Exception) {
            null
        }

        val ok = json?.optBoolean("ok", false) ?: false
        if (statusCode in 200..299 && ok) {
            return Result.Success
        }

        val description = json?.optString("description", "")?.takeIf { it.isNotBlank() }
        val message = description ?: "HTTP $statusCode: ${responseText.ifBlank { "no response body" }}"
        return Result.Failure(message)
    }

    companion object {
        private const val TAG = "TelegramSender"
        private const val SEND_ENDPOINT_TEMPLATE = "https://api.telegram.org/bot%s/sendMessage"
        private const val TIMEOUT_MS = 15000
    }
}
