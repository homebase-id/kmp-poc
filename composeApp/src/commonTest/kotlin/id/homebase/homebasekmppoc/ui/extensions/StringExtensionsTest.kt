package id.homebase.homebasekmppoc.ui.extensions

import kotlin.test.Test
import kotlin.test.assertEquals

class StringExtensionsTest {

    // Basic input tests
    @Test
    fun `empty string returns empty string`() {
        assertEquals("", "".cleanDomain())
    }

    @Test
    fun `whitespace only returns empty string`() {
        assertEquals("", "   ".cleanDomain())
    }

    @Test
    fun `simple domain is lowercased`() {
        assertEquals("example.com", "Example.COM".cleanDomain())
    }

    @Test
    fun `trims leading and trailing whitespace`() {
        assertEquals("example.com", "  example.com  ".cleanDomain())
    }

    // Protocol handling tests
    @Test
    fun `removes http protocol`() {
        assertEquals("example.com", "http://example.com".cleanDomain())
    }

    @Test
    fun `removes https protocol`() {
        assertEquals("example.com", "https://example.com".cleanDomain())
    }

    @Test
    fun `removes ftp protocol`() {
        assertEquals("example.com", "ftp://example.com".cleanDomain())
    }

    @Test
    fun `fixes missing slash in protocol`() {
        assertEquals("example.com", "https:/example.com".cleanDomain())
    }

    @Test
    fun `fixes missing colon in protocol`() {
        assertEquals("example.com", "https//example.com".cleanDomain())
    }

    @Test
    fun `collapses multiple slashes in protocol`() {
        assertEquals("example.com", "https:///example.com".cleanDomain())
    }

    // Path and query handling tests
    @Test
    fun `removes path after domain`() {
        assertEquals("example.com", "https://example.com/path/to/page".cleanDomain())
    }

    @Test
    fun `removes query parameters`() {
        assertEquals("example.com", "https://example.com?query=value".cleanDomain())
    }

    @Test
    fun `removes fragment identifier`() {
        assertEquals("example.com", "https://example.com#section".cleanDomain())
    }

    @Test
    fun `handles full URL with path query and fragment`() {
        assertEquals("example.com", "https://example.com/page?q=1#anchor".cleanDomain())
    }

    // Space and comma handling tests
    @Test
    fun `replaces spaces with periods`() {
        assertEquals("my.domain.com", "my domain com".cleanDomain())
    }

    @Test
    fun `replaces commas with periods`() {
        assertEquals("my.domain.com", "my,domain,com".cleanDomain())
    }

    // Illegal character removal tests
    @Test
    fun `removes hash and everything after it as fragment`() {
        assertEquals("example", "example#com".cleanDomain())
    }

    @Test
    fun `removes at symbol`() {
        assertEquals("examplecom", "example@com".cleanDomain())
    }

    @Test
    fun `removes special characters`() {
        // Note: # triggers fragment removal, so we test other special characters
        assertEquals("examplecom", "example!@$%^&*()com".cleanDomain())
    }

    @Test
    fun `removes brackets and braces`() {
        assertEquals("examplecom", "example[]{}<>com".cleanDomain())
    }

    @Test
    fun `removes quotes and backticks`() {
        // Space is converted to period then removed as duplicate
        assertEquals("example.com", "example'\"`.com".cleanDomain())
    }

    @Test
    fun `removes colon and semicolon`() {
        assertEquals("examplecom", "example:;com".cleanDomain())
    }

    @Test
    fun `removes pipe and tilde`() {
        assertEquals("examplecom", "example|~com".cleanDomain())
    }

    // Period normalization tests
    @Test
    fun `collapses multiple consecutive periods`() {
        assertEquals("example.com", "example...com".cleanDomain())
    }

    @Test
    fun `removes leading period`() {
        assertEquals("example.com", ".example.com".cleanDomain())
    }

    @Test
    fun `removes trailing period when preserveTrailingDot is false`() {
        assertEquals("example.com", "example.com.".cleanDomain(preserveTrailingDot = false))
    }

    @Test
    fun `preserves trailing period when preserveTrailingDot is true (default)`() {
        assertEquals("example.com.", "example.com.".cleanDomain())
    }

    @Test
    fun `removes leading and trailing periods when preserveTrailingDot is false`() {
        assertEquals("example.com", "..example.com..".cleanDomain(preserveTrailingDot = false))
    }

    @Test
    fun `removes leading but preserves trailing period when preserveTrailingDot is true`() {
        assertEquals("example.com.", "..example.com..".cleanDomain())
    }

    // Hyphen handling tests
    @Test
    fun `removes leading hyphen from label`() {
        assertEquals("example.com", "-example.com".cleanDomain())
    }

    @Test
    fun `removes trailing hyphen from label`() {
        assertEquals("example.com", "example-.com".cleanDomain())
    }

    @Test
    fun `removes leading and trailing hyphens from label`() {
        assertEquals("example.com", "-example-.com".cleanDomain())
    }

    @Test
    fun `collapses consecutive hyphens`() {
        assertEquals("my-example.com", "my--example.com".cleanDomain())
    }

    @Test
    fun `allows single hyphen in middle of label`() {
        assertEquals("my-example.com", "my-example.com".cleanDomain())
    }

    // Empty label handling tests
    @Test
    fun `removes empty labels caused by multiple periods`() {
        assertEquals("example.com", "example..com".cleanDomain())
    }

    // Unicode support tests
    @Test
    fun `preserves unicode characters for IDN`() {
        assertEquals("münchen.de", "München.de".cleanDomain())
    }

    @Test
    fun `handles unicode with protocol`() {
        assertEquals("münchen.de", "https://München.de".cleanDomain())
    }

    @Test
    fun `handles cyrillic characters`() {
        assertEquals("пример.рф", "ПРИМЕР.РФ".cleanDomain())
    }

    // Complex input tests
    @Test
    fun `handles complex pasted URL`() {
        assertEquals(
                "my-example.domain.com",
                "https://My--Example..DOMAIN.com/path?query=1#section".cleanDomain()
        )
    }

    @Test
    fun `handles badly formatted input with many issues`() {
        assertEquals("my.domain.com", "  HTTPS:///..--my, domain ,com--..  ".cleanDomain())
    }

    @Test
    fun `handles input with only illegal characters`() {
        assertEquals("", "@#$%^&*()".cleanDomain())
    }

    @Test
    fun `handles subdomain correctly`() {
        assertEquals("www.example.com", "https://www.example.com/".cleanDomain())
    }

    @Test
    fun `handles multiple subdomains`() {
        assertEquals("sub.domain.example.com", "sub.domain.example.com".cleanDomain())
    }

    // Edge cases
    @Test
    fun `handles only hyphens in label by removing it`() {
        assertEquals("example.com", "---.example.com".cleanDomain())
    }

    @Test
    fun `handles single character domain parts`() {
        assertEquals("a.b.c", "a.b.c".cleanDomain())
    }

    @Test
    fun `preserves numeric TLDs`() {
        assertEquals("example.123", "example.123".cleanDomain())
    }

    // Interactive typing tests (preserveTrailingDot = true, the default)
    @Test
    fun `allows single trailing dot for interactive typing`() {
        assertEquals("example.", "example.".cleanDomain())
    }

    @Test
    fun `allows trailing dot after partial domain entry`() {
        assertEquals("sub.example.", "sub.example.".cleanDomain())
    }

    @Test
    fun `collapses multiple trailing dots to one when preserving`() {
        assertEquals("example.", "example...".cleanDomain())
    }

    @Test
    fun `space at end converts to trailing dot`() {
        // User types "example " -> converts to "example."
        assertEquals("example.", "example ".cleanDomain())
    }

    @Test
    fun `comma at end converts to trailing dot`() {
        // User types "example," -> converts to "example."
        assertEquals("example.", "example,".cleanDomain())
    }

    @Test
    fun `multiple trailing spaces collapse to single dot`() {
        assertEquals("example.", "example   ".cleanDomain())
    }

    @Test
    fun `space in middle becomes dot and space at end becomes trailing dot`() {
        // User types "my example " -> "my.example."
        assertEquals("my.example.", "my example ".cleanDomain())
    }

    @Test
    fun `trailing space after subdomain allows continuing to type`() {
        // User types "sub " and then will type "example"
        assertEquals("sub.", "sub ".cleanDomain())
    }

    @Test
    fun `trailing comma after subdomain allows continuing to type`() {
        // User types "sub," and then will type "example"
        assertEquals("sub.", "sub,".cleanDomain())
    }

    @Test
    fun `trailing space is stripped when preserveTrailingDot is false`() {
        assertEquals("example", "example ".cleanDomain(preserveTrailingDot = false))
    }

    @Test
    fun `trailing comma is stripped when preserveTrailingDot is false`() {
        assertEquals("example", "example,".cleanDomain(preserveTrailingDot = false))
    }

    @Test
    fun `leading whitespace is always trimmed`() {
        assertEquals("example.", "   example.".cleanDomain())
    }

    @Test
    fun `leading and trailing whitespace with dot preserved`() {
        assertEquals("example.", "   example.   ".cleanDomain())
    }

    @Test
    fun `simulates typing domain character by character`() {
        // Simulates user typing "my.id" one character at a time
        assertEquals("m", "m".cleanDomain())
        assertEquals("my", "my".cleanDomain())
        assertEquals("my.", "my.".cleanDomain()) // Dot preserved for continuing
        assertEquals("my.i", "my.i".cleanDomain())
        assertEquals("my.id", "my.id".cleanDomain())
    }

    @Test
    fun `simulates typing domain with space as separator`() {
        // Simulates user typing "my id" using space as separator
        assertEquals("m", "m".cleanDomain())
        assertEquals("my", "my".cleanDomain())
        assertEquals("my.", "my ".cleanDomain()) // Space becomes dot, preserved
        assertEquals("my.i", "my i".cleanDomain()) // Space in middle becomes dot
        assertEquals("my.id", "my id".cleanDomain())
    }
}
