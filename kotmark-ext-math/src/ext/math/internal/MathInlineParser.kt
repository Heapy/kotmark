package io.heapy.kotmark.ext.math.internal

import io.heapy.kotmark.ext.math.InlineMath
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.beta.InlineContentParser
import io.heapy.kotmark.parser.beta.InlineContentParserFactory
import io.heapy.kotmark.parser.beta.InlineParserState
import io.heapy.kotmark.parser.beta.ParsedInline
import io.heapy.kotmark.parser.beta.Scanner

internal class MathInlineParser : InlineContentParser {
    override fun tryParse(inlineParserState: InlineParserState): ParsedInline? {
        val scanner = inlineParserState.scanner()
        val start = scanner.position()
        val count = scanner.matchMultiple('$')
        // Consume the whole run so no suffix of $$ or $$$ can open inline math.
        if (count != 1) {
            return ParsedInline.of(Text(scanner.getSource(start, scanner.position()).content), scanner.position())
        }
        if (scanner.peek() == Scanner.END || scanner.peek().isWhitespace()) return null
        val contentStart = scanner.position()
        while (scanner.hasNext() && scanner.peek() != '\n') {
            when (scanner.peek()) {
                '\\' -> {
                    scanner.next()
                    if (!scanner.hasNext() || scanner.peek() == '\n') return null
                    scanner.next()
                }
                '$' -> {
                    val contentEnd = scanner.position()
                    val previous = scanner.peekPreviousCodePoint()
                    val previousIsWhitespace = previous <= Char.MAX_VALUE.code && previous.toChar().isWhitespace()
                    val closingCount = scanner.matchMultiple('$')
                    // Only the nearest unescaped dollar run may close. Do not search
                    // past an invalid closer, which also bounds work on malformed input.
                    if (closingCount != 1 || previousIsWhitespace || scanner.peek() in '0'..'9') return null
                    return ParsedInline.of(
                        InlineMath(scanner.getSource(contentStart, contentEnd).content),
                        scanner.position(),
                    )
                }
                else -> scanner.next()
            }
        }
        return null
    }

    class Factory : InlineContentParserFactory {
        override val triggerCharacters: Set<Char> = setOf('$')

        override fun create(): InlineContentParser = MathInlineParser()
    }
}
