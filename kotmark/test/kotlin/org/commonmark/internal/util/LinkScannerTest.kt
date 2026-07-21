package org.commonmark.internal.util

import org.commonmark.parser.SourceLine
import org.commonmark.parser.SourceLines
import org.commonmark.parser.beta.Scanner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinkScannerTest {
    @Test
    fun linkLabelContent() {
        assertScan("label]", true, "label", LinkScanner::scanLinkLabelContent)
        assertScan("la\\]bel]", true, "la\\]bel", LinkScanner::scanLinkLabelContent)
        assertScan("la[bel]", false, "la", LinkScanner::scanLinkLabelContent)
    }

    @Test
    fun linkDestination() {
        assertScan("<destination>", true, "<destination>", LinkScanner::scanLinkDestination)
        assertScan("<destination", false, "<destination", LinkScanner::scanLinkDestination)
        assertScan("<dest\nation>", false, "<dest", LinkScanner::scanLinkDestination)
        assertScan("destination title", true, "destination", LinkScanner::scanLinkDestination)
        assertScan("a(b)c title", true, "a(b)c", LinkScanner::scanLinkDestination)
        assertScan("a(b)c)", true, "a(b)c", LinkScanner::scanLinkDestination)
        assertScan("", false, "", LinkScanner::scanLinkDestination)
    }

    @Test
    fun linkTitle() {
        assertScan("\"title\"", true, "\"title\"", LinkScanner::scanLinkTitle)
        assertScan("'title'", true, "'title'", LinkScanner::scanLinkTitle)
        assertScan("(title)", true, "(title)", LinkScanner::scanLinkTitle)
        assertScan("(ti(tle)", false, "(ti", LinkScanner::scanLinkTitle)
        assertScan("title", false, "", LinkScanner::scanLinkTitle)
    }

    private fun assertScan(
        input: String,
        expectedResult: Boolean,
        expectedConsumed: String,
        operation: (Scanner) -> Boolean,
    ) {
        val scanner = Scanner.of(SourceLines.of(SourceLine.of(input, null)))
        val start = scanner.position()
        if (expectedResult) {
            assertTrue(operation(scanner))
        } else {
            assertFalse(operation(scanner))
        }
        assertEquals(expectedConsumed, scanner.getSource(start, scanner.position()).content)
    }
}
