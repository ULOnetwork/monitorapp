package eu.ulonetwork.monitorapp.data

import android.util.Base64
import eu.ulonetwork.monitorapp.data.db.KeywordRule
import eu.ulonetwork.monitorapp.data.db.MatchMode
import org.json.JSONArray
import org.json.JSONObject

/**
 * Encodes/decodes a list of [KeywordRule]s as a compact, copy-pasteable text blob, the same way
 * [MailjetSettingsCodec] does for the Mailjet settings, so keyword rules can be transferred
 * between the owner's own devices without re-entering every rule by hand.
 *
 * `id` and `lastTriggeredAt` are deliberately not included: `id` is assigned fresh by Room on
 * import (imported rules are always added as new rows, never overwrite existing ones by ID), and
 * `lastTriggeredAt` is per-device runtime state that shouldn't carry over.
 */
object KeywordRulesCodec {
    private const val PREFIX = "UMRULES1:"

    fun encode(rules: List<KeywordRule>): String {
        val array = JSONArray()
        for (rule in rules) {
            val json = JSONObject().apply {
                put("keyword", rule.keyword)
                put("matchMode", rule.matchMode.name)
                put("caseSensitive", rule.caseSensitive)
                put("enabled", rule.enabled)
                put("notifyLocal", rule.notifyLocal)
                put("notifyEmail", rule.notifyEmail)
                put("notifyTelegram", rule.notifyTelegram)
                put("appPackageFilter", rule.appPackageFilter)
                put("screenGateKeyword", rule.screenGateKeyword)
                put("cooldownMinutes", rule.cooldownMinutes)
            }
            array.put(json)
        }
        val encoded = Base64.encodeToString(array.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        return PREFIX + encoded
    }

    /**
     * Returns the decoded rules (each with `id = 0` so Room assigns a fresh ID on insert, and
     * `lastTriggeredAt = null`), or null if [text] isn't a recognizable export blob.
     */
    fun decode(text: String): List<KeywordRule>? {
        val trimmed = text.trim()
        if (!trimmed.startsWith(PREFIX)) return null
        return try {
            val jsonBytes = Base64.decode(trimmed.removePrefix(PREFIX), Base64.NO_WRAP)
            val array = JSONArray(String(jsonBytes, Charsets.UTF_8))
            val rules = mutableListOf<KeywordRule>()
            for (i in 0 until array.length()) {
                val json = array.optJSONObject(i) ?: continue
                val keyword = json.optString("keyword", "")
                if (keyword.isBlank()) continue
                rules += KeywordRule(
                    id = 0,
                    keyword = keyword,
                    matchMode = MatchMode.entries.find { it.name == json.optString("matchMode") }
                        ?: MatchMode.CONTAINS,
                    caseSensitive = json.optBoolean("caseSensitive", false),
                    enabled = json.optBoolean("enabled", true),
                    notifyLocal = json.optBoolean("notifyLocal", true),
                    notifyEmail = json.optBoolean("notifyEmail", false),
                    notifyTelegram = json.optBoolean("notifyTelegram", false),
                    appPackageFilter = json.optString("appPackageFilter", "").ifBlank { null },
                    screenGateKeyword = json.optString("screenGateKeyword", "").ifBlank { null },
                    cooldownMinutes = json.optInt("cooldownMinutes", 10),
                    lastTriggeredAt = null
                )
            }
            rules.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }
}
