package id.homebase.homebasekmppoc.ui.extensions

/**
 * Cleans a domain string. Also built to handle a full string like a pasted URL, removing invalid
 * characters, and enforcing domain rules. Supports Unicode characters for IDNs (to be
 * Punycode-converted later) and handles common user input typos. It's intended to be called with
 * each character being input interactively (or pasted).
 * @return The cleaned domain string in lowercase.
 */
fun String.cleanDomain(): String {
    var cleanedString = this.trim()

    if (cleanedString.isEmpty()) {
        return ""
    }

    // Step 1: Handle pasted URLs - Strip protocols (http/https:// or similar), paths (after /), and
    // queries (after ?)
    cleanedString =
            cleanedString
                    // Normalize common protocol typos
                    .replace(
                            Regex("/{2,}"),
                            "//"
                    ) // Collapses multiple consecutive slashes (2+) to //
                    .replace(Regex(":/((?!/))"), "://") // Fix :/ to :// (missing one slash)
                    .replace(
                            Regex("^([\\w+-]+)(//)"),
                            "$1:$2"
                    ) // Fix scheme// to scheme:// (missing colon; no . in scheme)
                    // Remove general protocol (scheme:// where scheme can be any word-like string,
                    // but no . to avoid domain mismatches)
                    .replace(Regex("^[\\w+-]+://", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\?.*$"), "") // Remove query params after ?
                    .replace(Regex("#.*$"), "") // Remove fragments after # (handle URL anchors)
                    .replace(Regex("/.*$"), "") // Remove paths after /

    // Step 2: Replace spaces and commas with periods
    cleanedString = cleanedString.replace(" ", ".").replace(",", ".")

    // Step 3: Remove illegal characters (e.g., #, ?, /, \, &, %, @, !, *, (, ), [, ], {, }, :, ;,
    // ', ", <, >, =, +, ~, `, |, $, ^ )
    // but allow Unicode letters and digits (for later Punycode conversion)
    cleanedString = cleanedString.replace(Regex("[ #?/\\\\&%@!*()\\[\\]{}:;'\",<>+=~`|$^]"), "")

    // Step 4: Replace multiple consecutive periods with a single period
    cleanedString = cleanedString.replace(Regex("\\.{2,}"), ".")

    // Step 5: Enforce per-label rules (no start/end with '-', no consecutive '-')
    val labels = cleanedString.split(".")
    val cleanedLabels =
            labels.map { label ->
                // Remove leading/trailing '-', replace consecutive '-'
                label.replace(Regex("^-+|-+$"), "").replace(Regex("-{2,}"), "-")
            }
    cleanedString =
            cleanedLabels.filter { it.isNotEmpty() }.joinToString(".") // Remove empty labels

    // Step 6: Remove leading or trailing periods (good for valid domains)
    cleanedString = cleanedString.replace(Regex("^\\.|\\.$$"), "")

    // Step 7: Ensure lowercase (domains are case-insensitive)
    cleanedString = cleanedString.lowercase().trim()

    return cleanedString
}
