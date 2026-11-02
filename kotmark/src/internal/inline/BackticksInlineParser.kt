package io.heapy.kotmark.internal.inline

import io.heapy.kotmark.node.Code
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.beta.InlineContentParser
import io.heapy.kotmark.parser.beta.InlineContentParserFactory
import io.heapy.kotmark.parser.beta.InlineParserState
import io.heapy.kotmark.parser.beta.ParsedInline
import io.heapy.kotmark.text.Characters

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
