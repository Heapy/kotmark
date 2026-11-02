package io.heapy.kotmark.internal.inline

import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.beta.InlineContentParser
import io.heapy.kotmark.parser.beta.InlineContentParserFactory
import io.heapy.kotmark.parser.beta.InlineParserState
import io.heapy.kotmark.parser.beta.ParsedInline

/**
 * Parses URI and email autolinks in angle brackets.
 */
class AutolinkInlineParser : InlineContentParser {
    override fun tryParse(inlineParserState: InlineParserState): ParsedInline? {
        val scanner = inlineParserState.scanner()
        scanner.next()
        val textStart = scanner.position()
        if (scanner.find('>') > 0) {
            val textSource = scanner.getSource(textStart, scanner.position())
            val content = textSource.content
            scanner.next()

            val destination = when {
                URI.matches(content) -> content
                EMAIL.matches(content) -> "mailto:$content"
                else -> null
            }

            if (destination != null) {
                val link = Link(destination, null)
                val text = Text(content)
                text.setSourceSpans(textSource.sourceSpans)
                link.appendChild(text)
                return ParsedInline.of(link, scanner.position())
            }
        }
        return ParsedInline.none()
    }

    class Factory : InlineContentParserFactory {
        override val triggerCharacters: Set<Char> = setOf('<')

        override fun create(): InlineContentParser = AutolinkInlineParser()
    }

    companion object {
        private val URI = Regex("^[a-zA-Z][a-zA-Z0-9.+-]{1,31}:[^<>\\u0000-\\u0020]*$")
        private val EMAIL = Regex(
            "^([a-zA-Z0-9.!#\$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9]" +
                "(?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?" +
                "(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)$",
        )
    }
}
