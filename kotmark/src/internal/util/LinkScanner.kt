package io.heapy.kotmark.internal.util

import io.heapy.kotmark.parser.beta.Scanner
import kotlin.jvm.JvmStatic

object LinkScanner {
    /**
     * Scan link-label content, stopping at `]`, an invalid unescaped `[`, or end of input.
     */
    @JvmStatic
    fun scanLinkLabelContent(scanner: Scanner): Boolean {
        while (scanner.hasNext()) {
            when (scanner.peek()) {
                '\\' -> {
                    scanner.next()
                    if (isEscapable(scanner.peek())) {
                        scanner.next()
                    }
                }

                ']' -> return true
                '[' -> return false
                else -> scanner.next()
            }
        }
        return true
    }

    /**
     * Scan a link destination, stopping after the destination or returning false.
     */
    @JvmStatic
    fun scanLinkDestination(scanner: Scanner): Boolean {
        if (!scanner.hasNext()) {
            return false
        }

        if (!scanner.next('<')) {
            return scanLinkDestinationWithBalancedParens(scanner)
        }

        while (scanner.hasNext()) {
            when (scanner.peek()) {
                '\\' -> {
                    scanner.next()
                    if (isEscapable(scanner.peek())) {
                        scanner.next()
                    }
                }

                '\n', '<' -> return false
                '>' -> {
                    scanner.next()
                    return true
                }

                else -> scanner.next()
            }
        }
        return false
    }

    @JvmStatic
    fun scanLinkTitle(scanner: Scanner): Boolean {
        if (!scanner.hasNext()) {
            return false
        }

        val endDelimiter = when (scanner.peek()) {
            '"' -> '"'
            '\'' -> '\''
            '(' -> ')'
            else -> return false
        }
        scanner.next()

        if (!scanLinkTitleContent(scanner, endDelimiter) || !scanner.hasNext()) {
            return false
        }
        scanner.next()
        return true
    }

    @JvmStatic
    fun scanLinkTitleContent(scanner: Scanner, endDelimiter: Char): Boolean {
        while (scanner.hasNext()) {
            val current = scanner.peek()
            when {
                current == '\\' -> {
                    scanner.next()
                    if (isEscapable(scanner.peek())) {
                        scanner.next()
                    }
                }

                current == endDelimiter -> return true
                endDelimiter == ')' && current == '(' -> return false
                else -> scanner.next()
            }
        }
        return true
    }

    private fun scanLinkDestinationWithBalancedParens(scanner: Scanner): Boolean {
        var parens = 0
        var empty = true
        while (scanner.hasNext()) {
            when (val current = scanner.peek()) {
                ' ' -> return !empty
                '\\' -> {
                    scanner.next()
                    if (isEscapable(scanner.peek())) {
                        scanner.next()
                    }
                }

                '(' -> {
                    parens++
                    if (parens > MAX_NESTED_PARENS) {
                        return false
                    }
                    scanner.next()
                }

                ')' -> {
                    if (parens == 0) {
                        return true
                    }
                    parens--
                    scanner.next()
                }

                else -> {
                    if (current.isIsoControl()) {
                        return !empty
                    }
                    scanner.next()
                }
            }
            empty = false
        }
        return true
    }

    private fun isEscapable(c: Char): Boolean =
        c in '!'..'/' || c in ':'..'@' || c in '['..'`' || c in '{'..'~'

    private fun Char.isIsoControl(): Boolean =
        code in 0x0000..0x001F || code in 0x007F..0x009F

    private const val MAX_NESTED_PARENS = 32
}
