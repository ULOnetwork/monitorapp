package eu.ulonetwork.monitorapp.data.db

/**
 * Which edge of a [KeywordRule]'s matching state an [AlertLogEntry] represents.
 */
enum class AlertEventType {
    /** The rule's condition just started matching (was not matching before). */
    ISSUE,

    /** The rule's condition just stopped matching (was matching before). */
    RESOLVED
}
