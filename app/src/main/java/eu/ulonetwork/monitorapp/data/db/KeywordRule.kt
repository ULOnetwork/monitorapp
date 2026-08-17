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
    /** Package name this rule is limited to, or null to apply to every app. */
    val appPackageFilter: String? = null,
    val cooldownMinutes: Int = 10,
    val lastTriggeredAt: Long? = null,
    /** Whether the rule's condition is currently matching (an ISSUE alert has fired and no RESOLVED alert has fired since). */
    val issueActive: Boolean = false
)
