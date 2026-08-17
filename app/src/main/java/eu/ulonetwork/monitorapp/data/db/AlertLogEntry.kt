package eu.ulonetwork.monitorapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A record of a triggered [KeywordRule], stored for the user to review in the alert log screen.
 */
@Entity(tableName = "alert_log_entries")
data class AlertLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val ruleId: Long,
    val matchedKeyword: String,
    val appPackage: String,
    /** Short excerpt of the on-screen text around/containing the match, truncated. */
    val textSnippet: String,
    val notifiedLocal: Boolean,
    val notifiedEmail: Boolean,
    val eventType: AlertEventType,
    /** Failure message from [eu.ulonetwork.monitorapp.mail.MailjetMailSender], or null if e-mail wasn't attempted or succeeded. */
    val emailError: String? = null
)
