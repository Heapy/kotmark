package io.heapy.kotmark.internal.util

import java.io.Reader
import java.io.StringReader
import io.heapy.kotmark.internal.util.LineReader.Companion.CHAR_BUFFER_SIZE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

class LineReaderTest {
    @Test
    fun readsAllSupportedTerminatorsAcrossBufferBoundaries() {
        assertLines()
        assertLines("", "\n")
        assertLines("foo", "\n", "bar", "\n")
        assertLines("foo", "\n", "bar", null)
        assertLines("", "\n", "", "\n")
        assertLines("a".repeat(CHAR_BUFFER_SIZE - 1), "\n")
        assertLines("a".repeat(CHAR_BUFFER_SIZE), "\n")
        assertLines("a".repeat(CHAR_BUFFER_SIZE) + "b", "\n")

        assertLines("", "\r\n")
        assertLines("foo", "\r\n", "bar", "\r\n")
        assertLines("foo", "\r\n", "bar", null)
        assertLines("", "\r\n", "", "\r\n")
        assertLines("a".repeat(CHAR_BUFFER_SIZE - 2), "\r\n")
        assertLines("a".repeat(CHAR_BUFFER_SIZE - 1), "\r\n")
        assertLines("a".repeat(CHAR_BUFFER_SIZE), "\r\n")
        assertLines("a".repeat(CHAR_BUFFER_SIZE) + "b", "\r\n")

        assertLines("", "\r")
        assertLines("foo", "\r", "bar", "\r")
        assertLines("foo", "\r", "bar", null)
        assertLines("", "\r", "", "\r")
        assertLines("a".repeat(CHAR_BUFFER_SIZE - 1), "\r")
        assertLines("a".repeat(CHAR_BUFFER_SIZE), "\r")
        assertLines("a".repeat(CHAR_BUFFER_SIZE) + "b", "\r")

        assertLines("", "\n", "", "\r", "", "\r\n", "", "\n")
        assertLines(
            "what", "\r",
            "are", "\r",
            "", "\r",
            "you", "\r\n",
            "", "\r\n",
            "even", "\n",
            "doing", null,
        )
    }

    @Test
    fun closeIsIdempotentAndClosesDelegate() {
        val delegate = StringReader("test")
        val reader = LineReader(delegate)
        reader.close()
        reader.close()
        assertFails { delegate.read() }
    }

    private fun assertLines(vararg expectedParts: String?) {
        require(expectedParts.size % 2 == 0)
        val input = expectedParts.filterNotNull().joinToString("")
        assertLines(StringReader(input), expectedParts)
        assertLines(SlowStringReader(input), expectedParts)
    }

    private fun assertLines(
        reader: Reader,
        expectedParts: Array<out String?>,
    ) {
        val actual = mutableListOf<String?>()
        LineReader(reader).use { lineReader ->
            var line = lineReader.readLine()
            while (line != null) {
                actual += line
                actual += lineReader.lineTerminator
                line = lineReader.readLine()
            }
            assertNull(lineReader.lineTerminator)
        }
        assertEquals(expectedParts.toList(), actual)
    }

    private class SlowStringReader(
        private val value: String,
    ) : Reader() {
        private var position = 0
        private var empty = false

        override fun read(
            buffer: CharArray,
            offset: Int,
            length: Int,
        ): Int {
            if (length == 0) {
                return 0
            }
            empty = !empty
            if (empty) {
                return 0
            }
            if (position >= value.length) {
                return -1
            }
            buffer[offset] = value[position++]
            return 1
        }

        override fun close() = Unit
    }
}
