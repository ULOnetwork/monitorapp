package eu.ulonetwork.monitorapp.util

/**
 * Converts a simple glob-style keyword pattern into a [Regex] suitable for finding it anywhere
 * within a larger piece of text via [Regex.containsMatchIn] / [Regex.find].
 *
 * Supported wildcards:
 * - `*` matches any sequence of characters, including none (e.g. `koop*bitcoin` matches
 *   "koop nu snel bitcoin").
 * - `?` matches exactly one character.
 * - Any other character is treated literally; regex metacharacters in literal segments are
 *   escaped so they don't accidentally act as regex syntax.
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
    val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
    return Regex(regexPattern, options)
}
