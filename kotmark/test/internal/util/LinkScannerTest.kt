package io.heapy.kotmark.internal.util

import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.SourceLines
import io.heapy.kotmark.parser.beta.Scanner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LinkScannerTest {
    @Test
    fun linkLabelContent() {
        assertScan("label]", true, "label", LinkScanner::scanLinkLabelContent)
        assertScan("""la\]bel]""", true, """la\]bel""", LinkScanner::scanLinkLabelContent)
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
    fun unbalancedDestinationAtEof() {
        assertScan("a(b", false, "a(b", LinkScanner::scanLinkDestination)
    }

    @Test
    fun unbalancedDestinationBeforeSpace() {
        assertScan("a(b title", false, "a(b", LinkScanner::scanLinkDestination)
    }

    @Test
    fun unbalancedDestinationBeforeControl() {
        for (control in listOf('\t', '\n', '\u007F', '\u0085')) {
            assertScan("a(b${control}title", false, "a(b", LinkScanner::scanLinkDestination)
        }
        val scanner = Scanner.of(SourceLines.of(listOf(SourceLine.of("a(b", null), SourceLine.of("c", null))))
        assertFalse(LinkScanner.scanLinkDestination(scanner))
        assertEquals('\n', scanner.peek())
    }

    @Test
    fun balancedEscapedAndAngleBracketDestinationsRemainValid() {
        assertScan("a(b)", true, "a(b)", LinkScanner::scanLinkDestination)
        assertScan("a(b)\ttitle", true, "a(b)", LinkScanner::scanLinkDestination)
        assertScan("a\\(b", true, "a\\(b", LinkScanner::scanLinkDestination)
        assertScan("<a(b>", true, "<a(b>", LinkScanner::scanLinkDestination)
    }

    @Test
    fun linkTitle() {
        assertScan(""""title"""", true, """"title"""", LinkScanner::scanLinkTitle)
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
