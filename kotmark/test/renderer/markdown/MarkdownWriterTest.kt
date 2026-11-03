package io.heapy.kotmark.renderer.markdown

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarkdownWriterTest {
    @Test
    fun existingConstructorUsesLfAndMaintainsLineState() {
        val output = StringBuilder()
        val writer = MarkdownWriter(output)
        writer.raw("one")
        assertFalse(writer.isAtLineStart)
        writer.line()

        assertEquals("one\n", output.toString())
        assertEquals('\n', writer.lastChar)
        assertTrue(writer.isAtLineStart)
    }

    @Test
    fun separatorsBypassRawEscapesAndPrefixesPreserveState() {
        val output = StringBuilder()
        val writer = MarkdownWriter(output, "\r\n")
        writer.pushRawEscape { it == '\r' || it == '\n' }
        writer.pushPrefix("> ")
        writer.writePrefix("> ")
        writer.raw("one")
        writer.line()

        assertEquals("> one\r\n> ", output.toString())
        assertEquals(' ', writer.lastChar)
        assertTrue(writer.isAtLineStart)

        writer.raw("two")
        writer.block()
        assertTrue(writer.isAtLineStart)
        writer.raw("three")
        assertEquals("> one\r\n> two\r\n> \r\n> three", output.toString())
        assertEquals('e', writer.lastChar)
        assertFalse(writer.isAtLineStart)

        writer.pushTight(true)
        writer.block()
        writer.raw("four")
        assertEquals("> one\r\n> two\r\n> \r\n> three\r\n> four", output.toString())
    }

    @Test
    fun configuredSeparatorDoesNotRewriteRawText() {
        val output = StringBuilder()
        val writer = MarkdownWriter(output, "\r\n")
        writer.raw("one\ntwo\r\nthree")
        writer.line()

        assertEquals("one\ntwo\r\nthree\r\n", output.toString())
        assertEquals('\n', writer.lastChar)
        assertTrue(writer.isAtLineStart)
    }

    @Test
    fun emptySeparatorFailsWhenEmitted() {
        val writer = MarkdownWriter(StringBuilder(), "")
        writer.raw("text")
        assertFailsWith<IndexOutOfBoundsException> { writer.line() }
    }
}
