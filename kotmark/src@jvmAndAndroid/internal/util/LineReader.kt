package io.heapy.kotmark.internal.util

import java.io.Closeable
import java.io.Reader

class LineReader(
    reader: Reader,
) : Closeable {
    private var reader: Reader? = reader
    private var buffer: CharArray? = CharArray(CHAR_BUFFER_SIZE)
    private var position = 0
    private var limit = 0

    var lineTerminator: String? = null
        private set

    fun readLine(): String? {
        var result: StringBuilder? = null
        var sawCarriageReturn = false

        while (true) {
            if (position >= limit) {
                fill()
            }
            val characters = buffer

            if (sawCarriageReturn) {
                return if (
                    position < limit &&
                    requireNotNull(characters)[position] == '\n'
                ) {
                    position++
                    line(requireNotNull(result).toString(), "\r\n")
                } else {
                    line(requireNotNull(result).toString(), "\r")
                }
            }

            if (position >= limit) {
                return line(result?.toString(), null)
            }

            val start = position
            var index = position
            while (index < limit) {
                when (requireNotNull(characters)[index]) {
                    '\n' -> {
                        position = index + 1
                        return line(finish(result, start, index), "\n")
                    }
                    '\r' -> {
                        if (index + 1 < limit) {
                            return if (characters[index + 1] == '\n') {
                                position = index + 2
                                line(finish(result, start, index), "\r\n")
                            } else {
                                position = index + 1
                                line(finish(result, start, index), "\r")
                            }
                        }
                        sawCarriageReturn = true
                        position = index + 1
                        break
                    }
                }
                index++
            }

            if (position < index) {
                position = index
            }
            if (result == null) {
                result = StringBuilder(EXPECTED_LINE_LENGTH)
            }
            result.append(characters, start, index - start)
        }
    }

    override fun close() {
        val currentReader = reader ?: return
        try {
            currentReader.close()
        } finally {
            reader = null
            buffer = null
        }
    }

    private fun fill() {
        var read: Int
        val currentReader = checkNotNull(reader) { "LineReader is closed" }
        val characters = checkNotNull(buffer) { "LineReader is closed" }
        do {
            read = currentReader.read(characters, 0, characters.size)
        } while (read == 0)
        if (read > 0) {
            limit = read
            position = 0
        }
    }

    private fun line(
        line: String?,
        terminator: String?,
    ): String? {
        lineTerminator = terminator
        return line
    }

    private fun finish(
        result: StringBuilder?,
        start: Int,
        end: Int,
    ): String {
        val characters = requireNotNull(buffer)
        val length = end - start
        return result?.append(characters, start, length)?.toString()
            ?: String(characters, start, length)
    }

    companion object {
        const val CHAR_BUFFER_SIZE: Int = 8192
        const val EXPECTED_LINE_LENGTH: Int = 80
    }
}
