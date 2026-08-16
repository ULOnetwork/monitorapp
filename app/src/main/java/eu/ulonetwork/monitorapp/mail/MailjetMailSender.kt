package eu.ulonetwork.monitorapp.mail

import android.util.Base64
import android.util.Log
import eu.ulonetwork.monitorapp.data.MailjetSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import javax.net.ssl.HttpsURLConnection

/**
 * Sends alert e-mails via the Mailjet Send API v3.1 (https://api.mailjet.com/v3.1/send) over
 * plain HTTPS, using [HttpsURLConnection] and [org.json] only (no OkHttp/Retrofit dependency).
 *
 * This replaces a previous SMTP-based sender: on the target device, outbound SMTP ports
 * (25/465/587) are blocked by the network firewall regardless of host/port/TLS configuration,
 * while HTTPS (443) is not, so a REST-over-HTTPS mail API is the only viable transport here.
 *
 * All network I/O runs on [Dispatchers.IO].
 */
class MailjetMailSender {

    sealed class Result {
        data object Success : Result()
        data class Failure(val message: String) : Result()
    }

    suspend fun send(settings: MailjetSettings, subject: String, body: String): Result =
        withContext(Dispatchers.IO) {
            if (!settings.isComplete()) {
                return@withContext Result.Failure("Mailjet settings are incomplete")
            }

            var connection: HttpsURLConnection? = null
            try {
                val url = URL(SEND_ENDPOINT)
                connection = (url.openConnection() as HttpsURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Authorization", basicAuthHeader(settings.apiKey, settings.secretKey))
                }

                val requestBody = buildRequestBody(settings, subject, body)
                connection.outputStream.use { it.write(requestBody.toByteArray(StandardCharsets.UTF_8)) }

                val statusCode = connection.responseCode
                val responseStream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
                val responseText = readStream(responseStream)

                parseResult(statusCode, responseText)
            } catch (e: Exception) {
                val diagnostic = "${e.javaClass.simpleName}: ${e.message ?: "no further details"}"
                Log.e(TAG, "Failed to send e-mail via Mailjet: $diagnostic", e)
                Result.Failure(diagnostic)
            } finally {
                connection?.disconnect()
            }
        }

    private fun basicAuthHeader(apiKey: String, secretKey: String): String {
        val credentials = "$apiKey:$secretKey"
        val encoded = Base64.encodeToString(credentials.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        return "Basic $encoded"
    }

    private fun buildRequestBody(settings: MailjetSettings, subject: String, body: String): String {
        val from = JSONObject().apply {
            put("Email", settings.fromAddress)
            if (settings.fromName.isNotBlank()) {
                put("Name", settings.fromName)
            }
        }
        val to = JSONArray().put(JSONObject().put("Email", settings.toAddress))
        val message = JSONObject().apply {
            put("From", from)
            put("To", to)
            put("Subject", subject)
            put("TextPart", body)
        }
        val root = JSONObject().apply {
            put("Messages", JSONArray().put(message))
        }
        return root.toString()
    }

    private fun readStream(stream: InputStream?): String {
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { it.readText() }
    }

    /**
     * Parses Mailjet's JSON response body. Mailjet can respond HTTP 200 for the request itself
     * while an individual entry in "Messages" still reports `"Status": "error"` with the actual
     * failure details nested inside — that case must be treated as a failure too, not just a
     * non-2xx HTTP status.
     */
    private fun parseResult(statusCode: Int, responseText: String): Result {
        val json = try {
            if (responseText.isNotBlank()) JSONObject(responseText) else null
        } catch (e: Exception) {
            null
        }

        if (statusCode !in 200..299) {
            val message = extractErrorMessage(json) ?: "HTTP $statusCode: ${responseText.ifBlank { "no response body" }}"
            return Result.Failure(message)
        }

        val messages = json?.optJSONArray("Messages")
        if (messages != null) {
            for (i in 0 until messages.length()) {
                val messageResult = messages.optJSONObject(i) ?: continue
                val status = messageResult.optString("Status")
                if (!status.equals("success", ignoreCase = true)) {
                    val message = extractErrorMessage(messageResult)
                        ?: extractErrorMessage(json)
                        ?: "Mailjet reported status \"$status\""
                    return Result.Failure(message)
                }
            }
        }

        return Result.Success
    }

    /**
     * Digs the human-readable error description out of a Mailjet error payload, which can take a
     * few different shapes depending on whether the whole request was rejected (top-level
     * "ErrorMessage"/"ErrorInfo") or an individual message within a 200 response was ("Errors"
     * array, each entry carrying its own "ErrorMessage"/"ErrorInfo").
     */
    private fun extractErrorMessage(json: JSONObject?): String? {
        if (json == null) return null

        json.optString("ErrorMessage", "").takeIf { it.isNotBlank() }?.let { return it }
        json.optString("ErrorInfo", "").takeIf { it.isNotBlank() }?.let { return it }

        val errors = json.optJSONArray("Errors")
        if (errors != null && errors.length() > 0) {
            val parts = mutableListOf<String>()
            for (i in 0 until errors.length()) {
                val error = errors.optJSONObject(i) ?: continue
                val message = error.optString("ErrorMessage", "").ifBlank { error.optString("ErrorInfo", "") }
                if (message.isNotBlank()) parts.add(message)
            }
            if (parts.isNotEmpty()) return parts.joinToString("; ")
        }

        return null
    }

    companion object {
        private const val TAG = "MailjetMailSender"
        private const val SEND_ENDPOINT = "https://api.mailjet.com/v3.1/send"
        private const val TIMEOUT_MS = 15000
    }
}
