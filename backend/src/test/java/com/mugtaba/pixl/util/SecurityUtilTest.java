package com.mugtaba.pixl.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SecurityUtil Unit Tests")
class SecurityUtilTest {

    @Test
    @DisplayName("sanitizeInput_WithNullInput_ReturnsNull")
    void sanitizeInput_WithNullInput_ReturnsNull() {
        // Act
        String result = SecurityUtil.sanitizeInput(null);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("sanitizeInput_WithCleanInput_ReturnsUnchanged")
    void sanitizeInput_WithCleanInput_ReturnsUnchanged() {
        // Arrange
        String input = "This is clean text";

        // Act
        String result = SecurityUtil.sanitizeInput(input);

        // Assert
        assertThat(result).isEqualTo("This is clean text");
    }

    @Test
    @DisplayName("sanitizeInput_WithScriptTags_RemovesScriptTags")
    void sanitizeInput_WithScriptTags_RemovesScriptTags() {
        // Arrange
        String input = "Hello <script>alert('xss')</script> World";

        // Act
        String result = SecurityUtil.sanitizeInput(input);

        // Assert
        assertThat(result).doesNotContain("<script>")
                .doesNotContain("</script>")
                .contains("Hello")
                .contains("World");
    }

    @Test
    @DisplayName("sanitizeInput_WithHtmlSpecialChars_EscapesChars")
    void sanitizeInput_WithHtmlSpecialChars_EscapesChars() {
        // Arrange
        String input = "<div>Test & \"Quote\" 'Single'</div>";

        // Act
        String result = SecurityUtil.sanitizeInput(input);

        // Assert
        assertThat(result).contains("&lt;")
                .contains("&gt;")
                .contains("&amp;")
                .contains("&quot;")
                .contains("&#x27;");
    }

    @Test
    @DisplayName("sanitizeInput_WithAmpersand_EscapesAmpersand")
    void sanitizeInput_WithAmpersand_EscapesAmpersand() {
        // Arrange
        String input = "Tom & Jerry";

        // Act
        String result = SecurityUtil.sanitizeInput(input);

        // Assert
        assertThat(result).isEqualTo("Tom &amp; Jerry");
    }

    @Test
    @DisplayName("sanitizeInput_WithLessThan_EscapesLessThan")
    void sanitizeInput_WithLessThan_EscapesLessThan() {
        // Arrange
        String input = "5 < 10";

        // Act
        String result = SecurityUtil.sanitizeInput(input);

        // Assert
        assertThat(result).isEqualTo("5 &lt; 10");
    }

    @Test
    @DisplayName("sanitizeInput_WithGreaterThan_EscapesGreaterThan")
    void sanitizeInput_WithGreaterThan_EscapesGreaterThan() {
        // Arrange
        String input = "10 > 5";

        // Act
        String result = SecurityUtil.sanitizeInput(input);

        // Assert
        assertThat(result).isEqualTo("10 &gt; 5");
    }

    @Test
    @DisplayName("sanitizeInput_WithDoubleQuote_EscapesDoubleQuote")
    void sanitizeInput_WithDoubleQuote_EscapesDoubleQuote() {
        // Arrange
        String input = "He said \"Hello\"";

        // Act
        String result = SecurityUtil.sanitizeInput(input);

        // Assert
        assertThat(result).isEqualTo("He said &quot;Hello&quot;");
    }

    @Test
    @DisplayName("sanitizeInput_WithSingleQuote_EscapesSingleQuote")
    void sanitizeInput_WithSingleQuote_EscapesSingleQuote() {
        // Arrange
        String input = "It's a test";

        // Act
        String result = SecurityUtil.sanitizeInput(input);

        // Assert
        assertThat(result).isEqualTo("It&#x27;s a test");
    }

    @Test
    @DisplayName("sanitizeInput_WithForwardSlash_EscapesForwardSlash")
    void sanitizeInput_WithForwardSlash_EscapesForwardSlash() {
        // Arrange
        String input = "path/to/file";

        // Act
        String result = SecurityUtil.sanitizeInput(input);

        // Assert
        assertThat(result).isEqualTo("path&#x2F;to&#x2F;file");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert('xss')</script>",
            "<SCRIPT>alert('xss')</SCRIPT>",
            "<ScRiPt>alert('xss')</ScRiPt>"
    })
    @DisplayName("sanitizeInput_WithScriptTagsVariousCase_RemovesScriptTags")
    void sanitizeInput_WithScriptTagsVariousCase_RemovesScriptTags(String input) {
        // Act
        String result = SecurityUtil.sanitizeInput(input);

        // Assert
        assertThat(result).doesNotContainIgnoringCase("<script>")
                .doesNotContainIgnoringCase("</script>");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "\t"})
    @DisplayName("sanitizeInput_WithEmptyOrWhitespace_ReturnsAsIs")
    void sanitizeInput_WithEmptyOrWhitespace_ReturnsAsIs(String input) {
        // Act
        String result = SecurityUtil.sanitizeInput(input);

        // Assert
        if (input == null) {
            assertThat(result).isNull();
        } else {
            assertThat(result).isEqualTo(input);
        }
    }

    @Test
    @DisplayName("sanitizeInput_WithMultipleSpecialChars_EscapesAll")
    void sanitizeInput_WithMultipleSpecialChars_EscapesAll() {
        // Arrange
        String input = "<div class=\"test\">'Hello' & 'World' / End</div>";

        // Act
        String result = SecurityUtil.sanitizeInput(input);

        // Assert
        assertThat(result).contains("&lt;")
                .contains("&gt;")
                .contains("&amp;")
                .contains("&quot;")
                .contains("&#x27;")
                .contains("&#x2F;");
    }

    @Test
    @DisplayName("sanitizeInput_WithComplexXSSAttempt_RemovesAndEscapes")
    void sanitizeInput_WithComplexXSSAttempt_RemovesAndEscapes() {
        // Arrange
        String input = "<img src=x onerror=alert('XSS')><script>alert('XSS')</script>";

        // Act
        String result = SecurityUtil.sanitizeInput(input);

        // Assert
        assertThat(result).doesNotContain("<script>")
                .doesNotContain("</script>")
                .contains("&lt;")
                .contains("&gt;");
    }

    @Test
    @DisplayName("sanitizeInput_WithNestedScriptTags_RemovesScriptTags")
    void sanitizeInput_WithNestedScriptTags_RemovesScriptTags() {
        // Arrange
        String input = "<div><script>alert('test')</script></div>";

        // Act
        String result = SecurityUtil.sanitizeInput(input);

        // Assert
        assertThat(result).doesNotContain("<script>")
                .doesNotContain("</script>");
    }

    @Test
    @DisplayName("sanitizeInput_WithLongInput_ProcessesCorrectly")
    void sanitizeInput_WithLongInput_ProcessesCorrectly() {
        // Arrange
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Test <>&\"' ");
        }
        String input = sb.toString();

        // Act
        String result = SecurityUtil.sanitizeInput(input);

        // Assert
        assertThat(result).contains("&lt;")
                .contains("&gt;")
                .contains("&amp;")
                .contains("&quot;")
                .contains("&#x27;");
    }

    @Test
    @DisplayName("sanitizeInput_WithUnicodeCharacters_PreservesUnicode")
    void sanitizeInput_WithUnicodeCharacters_PreservesUnicode() {
        // Arrange
        String input = "Hello 世界 مرحبا";

        // Act
        String result = SecurityUtil.sanitizeInput(input);

        // Assert
        assertThat(result).isEqualTo("Hello 世界 مرحبا");
    }
}
