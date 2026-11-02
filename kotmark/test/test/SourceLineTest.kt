package io.heapy.kotmark.test

import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.parser.SourceLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SourceLineTest {
    @Test
    fun substring() {
        val line = SourceLine.of("abcd", SourceSpan.of(3, 10, 13, 4))

        assertSourceLine(line.substring(0, 4), "abcd", SourceSpan.of(3, 10, 13, 4))
        assertSourceLine(line.substring(0, 3), "abc", SourceSpan.of(3, 10, 13, 3))
        assertSourceLine(line.substring(0, 2), "ab", SourceSpan.of(3, 10, 13, 2))
        assertSourceLine(line.substring(0, 1), "a", SourceSpan.of(3, 10, 13, 1))
        assertSourceLine(line.substring(0, 0), "", null)

        assertSourceLine(line.substring(1, 4), "bcd", SourceSpan.of(3, 11, 14, 3))
        assertSourceLine(line.substring(1, 3), "bc", SourceSpan.of(3, 11, 14, 2))

        assertSourceLine(line.substring(3, 4), "d", SourceSpan.of(3, 13, 16, 1))
        assertSourceLine(line.substring(4, 4), "", null)
    }

    @Test
    fun substringBeginOutOfBounds() {
        val sourceLine = SourceLine.of("abcd", SourceSpan.of(3, 10, 13, 4))
        assertFailsWith<IndexOutOfBoundsException> {
            sourceLine.substring(3, 2)
        }
    }

    @Test
    fun substringEndOutOfBounds() {
        val sourceLine = SourceLine.of("abcd", SourceSpan.of(3, 10, 13, 4))
        assertFailsWith<IndexOutOfBoundsException> {
            sourceLine.substring(0, 5)
        }
    }

    private fun assertSourceLine(
        sourceLine: SourceLine,
        expectedContent: String,
        expectedSourceSpan: SourceSpan?,
    ) {
        assertEquals(expectedContent, sourceLine.content)
        assertEquals(expectedSourceSpan, sourceLine.sourceSpan)
    }
}
