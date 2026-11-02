package io.heapy.kotmark.internal.inline

import io.heapy.kotmark.internal.util.Html5Entities
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.beta.InlineContentParser
import io.heapy.kotmark.parser.beta.InlineContentParserFactory
import io.heapy.kotmark.parser.beta.InlineParserState
import io.heapy.kotmark.parser.beta.ParsedInline
import io.heapy.kotmark.parser.beta.Position
import io.heapy.kotmark.parser.beta.Scanner
import io.heapy.kotmark.text.asciiMatcher

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
        private val HEX = asciiMatcher {
            range('0', '9')
            range('A', 'F')
            range('a', 'f')
        }
        private val DECIMAL = asciiMatcher { range('0', '9') }
        private val ENTITY_START = asciiMatcher {
            range('A', 'Z')
            range('a', 'z')
        }
        private val ENTITY_CONTINUE = asciiMatcher(ENTITY_START) { range('0', '9') }
    }
}
