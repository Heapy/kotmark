package org.commonmark.internal.inline

import org.commonmark.internal.util.Html5Entities
import org.commonmark.node.Text
import org.commonmark.parser.beta.InlineContentParser
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.InlineParserState
import org.commonmark.parser.beta.ParsedInline
import org.commonmark.parser.beta.Position
import org.commonmark.parser.beta.Scanner
import org.commonmark.text.AsciiMatcher

class EntityInlineParser : InlineContentParser {
    override fun tryParse(inlineParserState: InlineParserState): ParsedInline? {
        val scanner = inlineParserState.scanner()
        val start = scanner.position()
        scanner.next()

        val current = scanner.peek()
        if (current == '#') {
            scanner.next()
            if (scanner.next('x') || scanner.next('X')) {
                val digits = scanner.match(HEX)
                if (digits in 1..6 && scanner.next(';')) {
                    return entity(scanner, start)
                }
            } else {
                val digits = scanner.match(DECIMAL)
                if (digits in 1..7 && scanner.next(';')) {
                    return entity(scanner, start)
                }
            }
        } else if (ENTITY_START.matches(current)) {
            scanner.match(ENTITY_CONTINUE)
            if (scanner.next(';')) {
                return entity(scanner, start)
            }
        }

        return ParsedInline.none()
    }

    private fun entity(scanner: Scanner, start: Position): ParsedInline {
        val text = scanner.getSource(start, scanner.position()).content
        return ParsedInline.of(Text(Html5Entities.entityToString(text)), scanner.position())
    }

    class Factory : InlineContentParserFactory {
        override val triggerCharacters: Set<Char> = setOf('&')

        override fun create(): InlineContentParser = EntityInlineParser()
    }

    companion object {
        private val HEX = AsciiMatcher.builder().range('0', '9').range('A', 'F').range('a', 'f').build()
        private val DECIMAL = AsciiMatcher.builder().range('0', '9').build()
        private val ENTITY_START = AsciiMatcher.builder().range('A', 'Z').range('a', 'z').build()
        private val ENTITY_CONTINUE = ENTITY_START.newBuilder().range('0', '9').build()
    }
}
