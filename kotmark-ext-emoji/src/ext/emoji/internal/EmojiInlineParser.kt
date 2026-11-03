package io.heapy.kotmark.ext.emoji.internal

import io.heapy.kotmark.ext.emoji.Emoji
import io.heapy.kotmark.ext.emoji.isShortcodeCharacter
import io.heapy.kotmark.parser.beta.InlineContentParser
import io.heapy.kotmark.parser.beta.InlineContentParserFactory
import io.heapy.kotmark.parser.beta.InlineParserState
import io.heapy.kotmark.parser.beta.ParsedInline

internal class EmojiInlineParser(
    private val aliases: Map<String, String>,
    private val maxLength: Int,
) : InlineContentParser {
    override fun tryParse(inlineParserState: InlineParserState): ParsedInline? {
        val scanner = inlineParserState.scanner()
        // InlineParserImpl restores the cursor when this attempt returns null.
        scanner.next()
        val start = scanner.position()
        var length = 0
        while (isShortcodeCharacter(scanner.peek())) {
            if (length == maxLength) return null
            scanner.next()
            length++
        }
        if (length == 0 || scanner.peek() != ':') return null
        val shortcode = scanner.getSource(start, scanner.position()).content
        val unicode = aliases[shortcode] ?: return null
        scanner.next()
        return ParsedInline.of(Emoji(shortcode, unicode), scanner.position())
    }

    class Factory(private val aliases: Map<String, String>) : InlineContentParserFactory {
        private val maxLength = aliases.keys.maxOfOrNull(String::length) ?: 0
        override val triggerCharacters: Set<Char> = setOf(':')

        override fun create(): InlineContentParser = EmojiInlineParser(aliases, maxLength)
    }
}
