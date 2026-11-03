package io.heapy.kotmark.internal.inline

import io.heapy.kotmark.node.Code
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.beta.InlineContentParser
import io.heapy.kotmark.parser.beta.InlineContentParserFactory
import io.heapy.kotmark.parser.beta.InlineParserState
import io.heapy.kotmark.parser.beta.ParsedInline
import io.heapy.kotmark.parser.beta.Position
import io.heapy.kotmark.text.Characters

class BackticksInlineParser : InlineContentParser {
    // Remember the last run of each length to avoid rescanning inputs with unmatched backticks.
    private val backtickPositions = mutableMapOf<Int, Position>()
    private var scannedToEnd = false

    override fun tryParse(inlineParserState: InlineParserState): ParsedInline {
        val scanner = inlineParserState.scanner()
        val start = scanner.position()
        val openingLength = scanner.matchMultiple('`')
        val afterOpening = scanner.position()

        if (scannedToEnd) {
            val potentialCloser = backtickPositions[openingLength]
            if (potentialCloser == null || potentialCloser <= start) {
                return ParsedInline.of(Text(scanner.getSource(start, afterOpening).content), afterOpening)
            }
        }

        while (scanner.find('`') > 0) {
            val beforeClosing = scanner.position()
            val length = scanner.matchMultiple('`')
            // After a full scan, a shorter successful scan must not replace the last run with
            // an earlier one, or later matching code spans would be incorrectly skipped.
            if (!scannedToEnd) {
                backtickPositions[length] = beforeClosing
            }
            if (length == openingLength) {
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

        // All potential closing runs have now been seen, regardless of their length.
        scannedToEnd = true
        val source = scanner.getSource(start, afterOpening)
        return ParsedInline.of(Text(source.content), afterOpening)
    }

    class Factory : InlineContentParserFactory {
        override val triggerCharacters: Set<Char> = setOf('`')

        override fun create(): InlineContentParser = BackticksInlineParser()
    }
}
