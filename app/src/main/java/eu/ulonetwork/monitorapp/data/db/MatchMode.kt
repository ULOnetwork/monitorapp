package eu.ulonetwork.monitorapp.data.db

/**
 * How a [KeywordRule.keyword] should be compared against the text extracted from screen.
 */
enum class MatchMode {
    /** Rule triggers when the keyword IS found in the screen text. */
    CONTAINS,

    /** Rule triggers when the keyword is NOT found in the screen text. Requires an app filter. */
    NOT_CONTAINS
}
