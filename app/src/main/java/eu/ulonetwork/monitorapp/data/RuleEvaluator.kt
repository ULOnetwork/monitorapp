package eu.ulonetwork.monitorapp.data

import android.content.Context
import android.util.Log
import eu.ulonetwork.monitorapp.R
import eu.ulonetwork.monitorapp.data.db.AlertEventType
import eu.ulonetwork.monitorapp.data.db.AlertLogEntry
import eu.ulonetwork.monitorapp.data.db.AppDatabase
import eu.ulonetwork.monitorapp.data.db.KeywordRule
import eu.ulonetwork.monitorapp.data.db.MatchMode
import eu.ulonetwork.monitorapp.mail.MailjetMailSender
import eu.ulonetwork.monitorapp.util.DeviceInfoProvider
import eu.ulonetwork.monitorapp.util.NotificationHelper
import eu.ulonetwork.monitorapp.util.buildKeywordRegex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Evaluates [KeywordRule]s against text extracted from the screen by the accessibility service,
 * logs matches and fires the configured alert channels.
 *
 * A rule's [matches] result represents whether its configured condition currently holds, already
 * accounting for CONTAINS vs. NOT_CONTAINS — `true` always means "as expected" (for CONTAINS: the
 * keyword is present; for NOT_CONTAINS: the keyword is absent). Alerting is edge-triggered off
 * that: a rule notifies ISSUE when the condition stops holding, stays silent while it remains
 * broken, and notifies RESOLVED when it holds again — never a RESOLVED without a preceding ISSUE.
 * [KeywordRule.issueActive] tracks which side of that transition a rule is currently on.
 */
class RuleEvaluator(private val context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val preferencesManager = PreferencesManager(context)
    private val mailSender = MailjetMailSender()

    /**
     * Evaluates all enabled rules against [screenText] observed while [appPackage] is in the
     * foreground. Suspends until logging/notifying for any state transitions has been handled.
     */
    suspend fun evaluate(appPackage: String, screenText: String) = withContext(Dispatchers.Default) {
        // Note: screenText may legitimately be blank (e.g. a sparse/loading screen, or the
        // accessibility tree exposed no text). Do NOT skip evaluation in that case — a
        // NOT_CONTAINS rule is specifically meant to fire when its keyword is absent, and a blank
        // screen is the clearest possible case of "absent".
        val rules = database.keywordRuleDao().getEnabledRules()
        val now = System.currentTimeMillis()

        for (rule in rules) {
            if (!appliesToApp(rule, appPackage)) continue
            if (!screenGateOpen(rule, screenText)) continue

            val isMatching = matches(rule, screenText)

            if (!rule.hasBaseline) {
                // First observation since this rule was created/edited/reset: we don't yet know
                // whether the condition holds, so just record the current state as the starting
                // point without alerting. Otherwise saving a rule (or a screen-gated rule simply
                // reaching its gated screen for the first time) before the monitored screen has
                // settled into its expected state would immediately look like a transition and
                // fire a false ISSUE.
                database.keywordRuleDao().establishBaseline(rule.id, issueActive = !isMatching)
                continue
            }

            when {
                !isMatching && !rule.issueActive && cooldownElapsed(rule, now) ->
                    deliverAlert(rule, appPackage, screenText, now, AlertEventType.ISSUE, newIssueActive = true)
                isMatching && rule.issueActive && cooldownElapsed(rule, now) ->
                    deliverAlert(rule, appPackage, screenText, now, AlertEventType.RESOLVED, newIssueActive = false)
                else -> {}
            }
        }
    }

    private fun appliesToApp(rule: KeywordRule, appPackage: String): Boolean {
        val filter = rule.appPackageFilter
        return filter.isNullOrBlank() || filter == appPackage
    }

    /**
     * Whether [rule] should be evaluated at all on the current screen. Without this gate, a rule
     * scoped only by [KeywordRule.appPackageFilter] would be checked against every screen inside
     * that app, including screens that never contain [KeywordRule.keyword] at all — misreading
     * "user navigated elsewhere in the app" as a real ISSUE/RESOLVED transition. When
     * [KeywordRule.screenGateKeyword] is set, the rule is only evaluated on screens where that
     * pattern is present; otherwise every screen of the matched app is evaluated, as before.
     */
    private fun screenGateOpen(rule: KeywordRule, screenText: String): Boolean {
        val gateKeyword = rule.screenGateKeyword
        if (gateKeyword.isNullOrBlank()) return true
        return buildKeywordRegex(gateKeyword, rule.caseSensitive).containsMatchIn(screenText)
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

    /**
     * Delivers a state transition (ISSUE or RESOLVED) for [rule]: updates its issue state,
     * shows the local notification and/or sends the Mailjet e-mail per the rule's configured
     * channels, and logs exactly one [AlertLogEntry].
     *
     * Runs inside [NonCancellable] because this is called from a coroutine job that the
     * accessibility service may cancel (it cancels the in-flight evaluation job whenever a new
     * screen-change event arrives, which can easily happen mid-flight during the Mailjet HTTPS
     * call). Once we've decided this is a real transition worth notifying about, delivery must
     * run to completion regardless.
     */
    private suspend fun deliverAlert(
        rule: KeywordRule,
        appPackage: String,
        screenText: String,
        now: Long,
        eventType: AlertEventType,
        newIssueActive: Boolean
    ) = withContext(NonCancellable) {
        val snippet = buildSnippet(screenText, rule)

        database.keywordRuleDao().updateIssueState(rule.id, newIssueActive, now)

        if (rule.notifyLocal) {
            NotificationHelper.showKeywordAlert(
                context, rule.keyword, eventType, appPackage, snippet, rule.id
            )
        }

        var notifiedEmail = false
        var emailError: String? = null

        if (rule.notifyEmail) {
            val settings = preferencesManager.getMailjetSettings()
            val subjectRes = if (eventType == AlertEventType.RESOLVED) {
                R.string.alert_email_subject_resolved
            } else {
                R.string.alert_email_subject_issue
            }
            val result = mailSender.send(
                settings = settings,
                subject = context.getString(subjectRes, rule.keyword),
                body = buildEmailBody(rule, appPackage, snippet, now, eventType)
            )
            notifiedEmail = result is MailjetMailSender.Result.Success
            if (result is MailjetMailSender.Result.Failure) {
                emailError = result.message
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
                notifiedEmail = notifiedEmail,
                eventType = eventType,
                emailError = emailError
            )
        )
    }

    private fun buildSnippet(screenText: String, rule: KeywordRule): String {
        val maxLen = 200
        if (rule.matchMode == MatchMode.NOT_CONTAINS) {
            return screenText.take(maxLen).ifBlank { context.getString(R.string.log_snippet_no_text) }
        }

        val regex = buildKeywordRegex(rule.keyword, rule.caseSensitive)
        val match = regex.find(screenText) ?: return screenText.take(maxLen).ifBlank { context.getString(R.string.log_snippet_no_text) }

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

    private fun buildEmailBody(
        rule: KeywordRule,
        appPackage: String,
        snippet: String,
        timestamp: Long,
        eventType: AlertEventType
    ): String {
        val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        val introRes = if (eventType == AlertEventType.RESOLVED) {
            R.string.alert_email_intro_resolved
        } else {
            R.string.alert_email_intro_issue
        }
        return buildString {
            appendLine(context.getString(R.string.alert_email_device_name_label, DeviceInfoProvider.deviceName()))
            appendLine(context.getString(R.string.alert_email_device_id_label, DeviceInfoProvider.deviceIdentifier(context)))
            appendLine(context.getString(R.string.alert_email_ip_label, DeviceInfoProvider.localIpAddress()))
            appendLine()
            appendLine(context.getString(introRes))
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
