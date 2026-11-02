package io.heapy.kotmark.internal.inline

import io.heapy.kotmark.node.HardLineBreak
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.beta.InlineContentParser
import io.heapy.kotmark.parser.beta.InlineContentParserFactory
import io.heapy.kotmark.parser.beta.InlineParserState
import io.heapy.kotmark.parser.beta.ParsedInline

class BackslashInlineParser : InlineContentParser {
    override fun tryParse(inlineParserState: InlineParserState): ParsedInline {
        val scanner = inlineParserState.scanner()
        scanner.next()

        val next = scanner.peek()
        return when {
            next == '\n' -> {
                scanner.next()
                ParsedInline.of(HardLineBreak(), scanner.position())
            }

            isEscapable(next) -> {
                scanner.next()
                ParsedInline.of(Text(next.toString()), scanner.position())
            }

            else -> ParsedInline.of(Text("\\"), scanner.position())
        }
    }

    class Factory : InlineContentParserFactory {
        override val triggerCharacters: Set<Char> = setOf('\\')

        override fun create(): InlineContentParser = BackslashInlineParser()
    }

    companion object {
        private fun isEscapable(c: Char): Boolean =
            c in '!'..'/' || c in ':'..'@' || c in '['..'`' || c in '{'..'~'
    }
}
