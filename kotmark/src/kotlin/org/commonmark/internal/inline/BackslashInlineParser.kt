package org.commonmark.internal.inline

import org.commonmark.node.HardLineBreak
import org.commonmark.node.Text
import org.commonmark.parser.beta.InlineContentParser
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.InlineParserState
import org.commonmark.parser.beta.ParsedInline

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
