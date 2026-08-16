package eu.ulonetwork.monitorapp.data

import android.content.Context
import android.util.Log
import eu.ulonetwork.monitorapp.R
import eu.ulonetwork.monitorapp.data.db.AlertLogEntry
import eu.ulonetwork.monitorapp.data.db.AppDatabase
import eu.ulonetwork.monitorapp.data.db.KeywordRule
import eu.ulonetwork.monitorapp.data.db.MatchMode
import eu.ulonetwork.monitorapp.mail.MailjetMailSender
import eu.ulonetwork.monitorapp.util.NotificationHelper
import eu.ulonetwork.monitorapp.util.buildKeywordRegex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Evaluates [KeywordRule]s against text extracted from the screen by the accessibility service,
 * logs matches and fires the configured alert channels.
 */
class RuleEvaluator(private val context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val preferencesManager = PreferencesManager(context)
    private val mailSender = MailjetMailSender()

    /**
     * Evaluates all enabled rules against [screenText] observed while [appPackage] is in the
     * foreground. Suspends until logging/notifying for any matches has been handled.
     */
    suspend fun evaluate(appPackage: String, screenText: String) = withContext(Dispatchers.Default) {
        if (screenText.isBlank()) return@withContext

        val rules = database.keywordRuleDao().getEnabledRules()
        val now = System.currentTimeMillis()

        for (rule in rules) {
            if (!appliesToApp(rule, appPackage)) continue
            if (!cooldownElapsed(rule, now)) continue

            val matched = matches(rule, screenText)
            if (matched) {
                handleMatch(rule, appPackage, screenText, now)
            }
        }
    }

    private fun appliesToApp(rule: KeywordRule, appPackage: String): Boolean {
        val filter = rule.appPackageFilter
        return filter.isNullOrBlank() || filter == appPackage
    }

    private fun cooldownElapsed(rule: KeywordRule, now: Long): Boolean {
        val last = rule.lastTriggeredAt ?: return true
        val cooldownMillis = TimeUnit.MINUTES.toMillis(rule.cooldownMinutes.toLong())
        return now - last >= cooldownMillis
    }

    private fun matches(rule: KeywordRule, screenText: String): Boolean {
        if (rule.keyword.isBlank()) return false

        val regex = buildKeywordRegex(rule.keyword, rule.caseSensitive)
        val contains = regex.containsMatchIn(screenText)
        return when (rule.matchMode) {
            MatchMode.CONTAINS -> contains
            MatchMode.NOT_CONTAINS -> !contains
        }
    }

    private suspend fun handleMatch(
        rule: KeywordRule,
        appPackage: String,
        screenText: String,
        now: Long
    ) {
        val snippet = buildSnippet(screenText, rule)

        database.keywordRuleDao().updateLastTriggeredAt(rule.id, now)

        var notifiedEmail = false

        if (rule.notifyLocal) {
            NotificationHelper.showKeywordAlert(context, rule.keyword, appPackage, snippet, rule.id)
        }

        if (rule.notifyEmail) {
            val settings = preferencesManager.getMailjetSettings()
            val result = mailSender.send(
                settings = settings,
                subject = context.getString(R.string.alert_email_subject, rule.keyword),
                body = buildEmailBody(rule, appPackage, snippet, now)
            )
            notifiedEmail = result is MailjetMailSender.Result.Success
            if (result is MailjetMailSender.Result.Failure) {
                Log.w(TAG, "E-mail alert failed for rule ${rule.id}: ${result.message}")
            }
        }

        database.alertLogDao().insert(
            AlertLogEntry(
                timestamp = now,
                ruleId = rule.id,
                matchedKeyword = rule.keyword,
                appPackage = appPackage,
                textSnippet = snippet,
                notifiedLocal = rule.notifyLocal,
                notifiedEmail = notifiedEmail
            )
        )
    }

    private fun buildSnippet(screenText: String, rule: KeywordRule): String {
        val maxLen = 200
        if (rule.matchMode == MatchMode.NOT_CONTAINS) {
            return screenText.take(maxLen)
        }

        val regex = buildKeywordRegex(rule.keyword, rule.caseSensitive)
        val match = regex.find(screenText) ?: return screenText.take(maxLen)

        val contextRadius = 80
        // match.range.last is inclusive; for a zero-length match (e.g. a pattern consisting of
        // only "*") it equals range.first - 1, so "+ 1" correctly yields range.first as the
        // exclusive end in that case too.
        val start = (match.range.first - contextRadius).coerceAtLeast(0)
        val end = (match.range.last + 1 + contextRadius).coerceAtMost(screenText.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < screenText.length) "…" else ""
        return (prefix + screenText.substring(start, end) + suffix).take(maxLen)
    }

    private fun buildEmailBody(rule: KeywordRule, appPackage: String, snippet: String, timestamp: Long): String {
        val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        return buildString {
            appendLine(context.getString(R.string.alert_email_intro))
            appendLine()
            appendLine(context.getString(R.string.alert_email_keyword_label, rule.keyword))
            appendLine(context.getString(R.string.alert_email_app_label, appPackage))
            appendLine(context.getString(R.string.alert_email_time_label, formatter.format(Date(timestamp))))
            appendLine()
            appendLine(context.getString(R.string.alert_email_fragment_label))
            appendLine(snippet)
        }
    }

    companion object {
        private const val TAG = "RuleEvaluator"
    }
}
