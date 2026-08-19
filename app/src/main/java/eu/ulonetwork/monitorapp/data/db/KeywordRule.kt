package eu.ulonetwork.monitorapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-configured rule that is evaluated against text extracted from the screen by the
 * accessibility service.
 */
@Entity(tableName = "keyword_rules")
data class KeywordRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val keyword: String,
    val matchMode: MatchMode = MatchMode.CONTAINS,
    val caseSensitive: Boolean = false,
    val enabled: Boolean = true,
    val notifyLocal: Boolean = true,
    val notifyEmail: Boolean = false,
    val notifyTelegram: Boolean = false,
    /** Package name this rule is limited to, or null to apply to every app. */
    val appPackageFilter: String? = null,
    /**
     * Optional pattern (same wildcard syntax as [keyword]) that must be present on screen before
     * this rule is evaluated at all. Lets a rule stay silent while the user is on an unrelated
     * screen within the same app, instead of misreading that screen's absence of [keyword] as a
     * real match/non-match.
     */
    val screenGateKeyword: String? = null,
    val cooldownMinutes: Int = 10,
    val lastTriggeredAt: Long? = null,
    /** Whether the rule's condition currently fails to hold (an ISSUE alert has fired and no RESOLVED alert has fired since). */
    val issueActive: Boolean = false,
    /**
     * Whether [issueActive] reflects a condition that has actually been observed to change, as
     * opposed to just the rule's freshly-reset default. A new or just-edited rule doesn't know
     * yet whether its condition currently holds, so its first evaluation only records that as the
     * starting baseline instead of alerting — otherwise saving a rule while the monitored screen
     * hasn't loaded/navigated to its expected state yet would immediately fire a false ISSUE.
     */
    val hasBaseline: Boolean = false
)
