package io.heapy.kotmark.test

import io.heapy.kotmark.renderer.text.TextContentWriter
import kotlin.test.Test
import kotlin.test.assertEquals

class TextContentWriterTest {
    @Test
    fun whitespace() {
        val stringBuilder = StringBuilder()
        val writer = TextContentWriter(stringBuilder)
        writer.write("foo")
        writer.whitespace()
        writer.write("bar")
        assertEquals("foo bar", stringBuilder.toString())
    }

    @Test
    fun colon() {
        val stringBuilder = StringBuilder()
        val writer = TextContentWriter(stringBuilder)
        writer.write("foo")
        writer.colon()
        writer.write("bar")
        assertEquals("foo:bar", stringBuilder.toString())
    }

    @Test
    fun line() {
        val stringBuilder = StringBuilder()
        val writer = TextContentWriter(stringBuilder)
        writer.write("foo")
        writer.line()
        writer.write("bar")
        assertEquals("foo\nbar", stringBuilder.toString())
    }

    @Test
    fun writeStripped() {
        val stringBuilder = StringBuilder()
        val writer = TextContentWriter(stringBuilder)
        writer.writeStripped("foo\n bar")
        assertEquals("foo bar", stringBuilder.toString())
    }

    @Test
    fun write() {
        val stringBuilder = StringBuilder()
        val writer = TextContentWriter(stringBuilder)
        writer.write("foo bar")
        assertEquals("foo bar", stringBuilder.toString())
    }
}
