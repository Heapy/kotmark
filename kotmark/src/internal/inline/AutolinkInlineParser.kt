package io.heapy.kotmark.internal.inline

import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.beta.InlineContentParser
import io.heapy.kotmark.parser.beta.InlineContentParserFactory
import io.heapy.kotmark.parser.beta.InlineParserState
import io.heapy.kotmark.parser.beta.ParsedInline
import io.heapy.kotmark.text.asciiMatcher

/**
 * Parses URI and email autolinks in angle brackets.
 */
class AutolinkInlineParser : InlineContentParser {
    override fun tryParse(inlineParserState: InlineParserState): ParsedInline? {
        val scanner = inlineParserState.scanner()
        scanner.next()
        val textStart = scanner.position()
        if (scanner.find(ANGLE_BRACKETS) > 0) {
            if (scanner.peek() != '>') return ParsedInline.none()

            val textSource = scanner.getSource(textStart, scanner.position())
            val content = textSource.content
            scanner.next()

            val destination = when {
                URI.matches(content) -> content
                isEmail(content) -> "mailto:$content"
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
        private val ANGLE_BRACKETS = asciiMatcher {
            c('<')
            c('>')
        }

        private val URI = Regex("""^[a-zA-Z][a-zA-Z0-9.+-]{1,31}:[^<>\u0000-\u0020]*$""")
        private val EMAIL_LOCAL_PART = Regex("""^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@""")
        private val EMAIL_DOMAIN_LABEL = Regex("[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?")

        private fun isEmail(content: String): Boolean {
            val localPart = EMAIL_LOCAL_PART.matchAt(content, 0) ?: return false
            var index = localPart.range.last + 1
            // Match bounded labels separately instead of recursing through a repeated regex group.
            while (true) {
                val label = EMAIL_DOMAIN_LABEL.matchAt(content, index) ?: return false
                index = label.range.last + 1
                if (index == content.length) return true
                if (content[index] != '.') return false
                index++
            }
        }
    }
}
