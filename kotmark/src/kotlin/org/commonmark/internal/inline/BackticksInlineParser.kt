package org.commonmark.internal.inline

import org.commonmark.node.Code
import org.commonmark.node.Text
import org.commonmark.parser.beta.InlineContentParser
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.InlineParserState
import org.commonmark.parser.beta.ParsedInline
import org.commonmark.text.Characters

class BackticksInlineParser : InlineContentParser {
    override fun tryParse(inlineParserState: InlineParserState): ParsedInline {
        val scanner = inlineParserState.scanner()
        val start = scanner.position()
        val openingTicks = scanner.matchMultiple('`')
        val afterOpening = scanner.position()

        while (scanner.find('`') > 0) {
            val beforeClosing = scanner.position()
            val count = scanner.matchMultiple('`')
            if (count == openingTicks) {
                var content = scanner.getSource(afterOpening, beforeClosing).content.replace('\n', ' ')
                if (
                    content.length >= 3 &&
                    content.first() == ' ' &&
                    content.last() == ' ' &&
                    Characters.hasNonSpace(content)
                ) {
                    content = content.substring(1, content.lastIndex)
                }

                return ParsedInline.of(Code(content), scanner.position())
            }
        }

        val source = scanner.getSource(start, afterOpening)
        return ParsedInline.of(Text(source.content), afterOpening)
    }

    class Factory : InlineContentParserFactory {
        override val triggerCharacters: Set<Char> = setOf('`')

        override fun create(): InlineContentParser = BackticksInlineParser()
    }
}
