package eu.ulonetwork.monitorapp.util

/**
 * Converts a simple glob-style keyword pattern into a [Regex] suitable for finding it anywhere
 * within a larger piece of text via [Regex.containsMatchIn] / [Regex.find].
 *
 * Supported wildcards:
 * - `*` matches any sequence of characters, including none and including line breaks (e.g.
 *   `koop*bitcoin` matches "koop nu snel bitcoin", and also matches across the newlines that
 *   separate distinct screen elements, e.g. `Attestation*Hardware-verified` matches
 *   "Attestation Level\nHardware-verified (Full)").
 * - `?` matches exactly one character, including a line break.
 * - Any other character is treated literally; regex metacharacters in literal segments are
 *   escaped so they don't accidentally act as regex syntax.
 *
 * The extracted screen text joins the text of separate UI elements with `\n` (see
 * `ScreenReaderAccessibilityService.collectText`), so a wildcard spanning two on-screen labels is
 * the common case, not an edge case. [RegexOption.DOT_MATCHES_ALL] is therefore always applied so
 * `.` (and `.*`/`.` from `*`/`?`) matches newlines too — without it, a pattern like
 * `Attestation*Hardware-verified` would silently fail to match text that visibly contains both
 * fragments, just because they landed in different accessibility nodes.
 *
 * A pattern with no wildcard characters behaves identically to a plain substring search: escaping
 * a literal string and searching with [Regex.containsMatchIn] is equivalent to [String.contains],
 * so this is fully backward compatible with existing plain-keyword rules.
 */
fun buildKeywordRegex(pattern: String, caseSensitive: Boolean): Regex {
    val regexPattern = buildString {
        for (char in pattern) {
            when (char) {
                '*' -> append(".*")
                '?' -> append(".")
                else -> append(Regex.escape(char.toString()))
            }
        }
    }
    val options = mutableSetOf(RegexOption.DOT_MATCHES_ALL)
    if (!caseSensitive) options += RegexOption.IGNORE_CASE
    return Regex(regexPattern, options)
}
