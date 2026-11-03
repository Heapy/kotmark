package io.heapy.kotmark.renderer.markdown

import io.heapy.kotmark.text.CharMatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarkdownWriterTest {
    @Test
    fun textEscaping() {
        val cases = mapOf(
            "" to "",
            "abc" to "abc",
            "*abc" to "\\*abc",
            "abc*" to "abc\\*",
            "a*b*c" to "a\\*b\\*c",
            "a**b" to "a\\*\\*b",
            "***" to "\\*\\*\\*",
            "*" to "\\*",
        )
        for ([input, expected] in cases) {
            assertEquals(expected, text(input) { it == '*' }, input)
        }
    }

    @Test
    fun textEscapingNewline() {
        assertEquals("a&#10;b&#10;c", text("a\nb\nc") { it == '\n' })
        assertEquals("&#10;", text("\n") { it == '\n' })
    }

    @Test
    fun rawEscaping() {
        val output = StringBuilder()
        val writer = MarkdownWriter(output)
        writer.pushRawEscape { it == '|' }
        writer.raw("a|b|c")
        writer.popRawEscape()
        writer.raw("d|e")
        assertEquals("a\\|b\\|cd|e", output.toString())
    }

    @Test
    fun escapedTextMaintainsLastCharAndLineState() {
        val writer = MarkdownWriter(StringBuilder())
        writer.text("") { it == '*' }
        assertTrue(writer.isAtLineStart)
        assertEquals('\u0000', writer.lastChar)
        writer.text("a*") { it == '*' }
        assertEquals('*', writer.lastChar)
        assertFalse(writer.isAtLineStart)
        writer.line()
        assertTrue(writer.isAtLineStart)
    }

    @Test
    fun unescapedSpansAreAppendedInBulk() {
        val output = RecordingAppendable()
        val writer = MarkdownWriter(output)
        writer.pushRawEscape { it == '|' || it == '\n' }
        writer.text("alpha*beta|gamma\ndelta") { it == '*' }
        writer.raw("epsilon|zeta")

        assertEquals("alpha\\*beta\\|gamma&#10;deltaepsilon\\|zeta", output.toString())
        assertEquals(listOf("alpha", "beta", "gamma", "delta", "epsilon", "zeta"), output.spans)
        assertEquals(listOf('\\', '*', '\\', '|', '\\', '|'), output.characters)
        assertEquals(listOf("&#10;"), output.sequences)
    }

    @Test
    fun defaultSeparatorUsesLfAndMaintainsLineState() {
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
        writer.block()
        assertFailsWith<IndexOutOfBoundsException> { writer.raw("next") }
    }

    private fun text(value: String, escape: CharMatcher): String =
        StringBuilder().also { MarkdownWriter(it).text(value, escape) }.toString()

    private class RecordingAppendable : Appendable {
        private val output = StringBuilder()
        val spans = mutableListOf<String>()
        val characters = mutableListOf<Char>()
        val sequences = mutableListOf<String>()

        override fun append(value: CharSequence?): Appendable {
            sequences.add(value.toString())
            output.append(value)
            return this
        }

        override fun append(value: CharSequence?, startIndex: Int, endIndex: Int): Appendable {
            spans.add(value!!.subSequence(startIndex, endIndex).toString())
            output.append(value, startIndex, endIndex)
            return this
        }

        override fun append(value: Char): Appendable {
            characters.add(value)
            output.append(value)
            return this
        }

        override fun toString(): String = output.toString()
    }
}
