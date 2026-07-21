package org.commonmark.internal.inline

import org.commonmark.node.HtmlInline
import org.commonmark.parser.beta.InlineContentParser
import org.commonmark.parser.beta.InlineContentParserFactory
import org.commonmark.parser.beta.InlineParserState
import org.commonmark.parser.beta.ParsedInline
import org.commonmark.parser.beta.Position
import org.commonmark.parser.beta.Scanner
import org.commonmark.text.AsciiMatcher

class HtmlInlineParser : InlineContentParser {
    override fun tryParse(inlineParserState: InlineParserState): ParsedInline? {
        val scanner = inlineParserState.scanner()
        val start = scanner.position()
        scanner.next()

        var current = scanner.peek()
        when {
            TAG_NAME_START.matches(current) && tryOpenTag(scanner) -> return htmlInline(start, scanner)
            current == '/' && tryClosingTag(scanner) -> return htmlInline(start, scanner)
            current == '?' && tryProcessingInstruction(scanner) -> return htmlInline(start, scanner)
            current == '!' -> {
                scanner.next()
                current = scanner.peek()
                when {
                    current == '-' && tryComment(scanner) -> return htmlInline(start, scanner)
                    current == '[' && tryCdata(scanner) -> return htmlInline(start, scanner)
                    ASCII_LETTER.matches(current) && tryDeclaration(scanner) -> return htmlInline(start, scanner)
                }
            }
        }
        return ParsedInline.none()
    }

    class Factory : InlineContentParserFactory {
        override val triggerCharacters: Set<Char> = setOf('<')

        override fun create(): InlineContentParser = HtmlInlineParser()
    }

    companion object {
        private val ASCII_LETTER = AsciiMatcher.builder().range('A', 'Z').range('a', 'z').build()
        private val TAG_NAME_START = ASCII_LETTER
        private val TAG_NAME_CONTINUE =
            TAG_NAME_START.newBuilder().range('0', '9').c('-').build()
        private val ATTRIBUTE_START =
            ASCII_LETTER.newBuilder().c('_').c(':').build()
        private val ATTRIBUTE_CONTINUE =
            ATTRIBUTE_START.newBuilder().range('0', '9').c('.').c('-').build()
        private val ATTRIBUTE_VALUE_END = AsciiMatcher.builder()
            .c(' ').c('\t').c('\n').c('\u000B').c('\u000C').c('\r')
            .c('"').c('\'').c('=').c('<').c('>').c('`')
            .build()

        private fun htmlInline(start: Position, scanner: Scanner): ParsedInline {
            val node = HtmlInline()
            node.literal = scanner.getSource(start, scanner.position()).content
            return ParsedInline.of(node, scanner.position())
        }

        private fun tryOpenTag(scanner: Scanner): Boolean {
            scanner.next()
            scanner.match(TAG_NAME_CONTINUE)
            var whitespace = scanner.whitespace() >= 1
            while (whitespace && scanner.match(ATTRIBUTE_START) >= 1) {
                scanner.match(ATTRIBUTE_CONTINUE)
                whitespace = scanner.whitespace() >= 1
                if (scanner.next('=')) {
                    scanner.whitespace()
                    when (scanner.peek()) {
                        '\'' -> {
                            scanner.next()
                            if (scanner.find('\'') < 0) return false
                            scanner.next()
                        }

                        '"' -> {
                            scanner.next()
                            if (scanner.find('"') < 0) return false
                            scanner.next()
                        }

                        else -> if (scanner.find(ATTRIBUTE_VALUE_END) <= 0) return false
                    }
                    whitespace = scanner.whitespace() >= 1
                }
            }

            scanner.next('/')
            return scanner.next('>')
        }

        private fun tryClosingTag(scanner: Scanner): Boolean {
            scanner.next()
            if (scanner.match(TAG_NAME_START) < 1) return false
            scanner.match(TAG_NAME_CONTINUE)
            scanner.whitespace()
            return scanner.next('>')
        }

        private fun tryProcessingInstruction(scanner: Scanner): Boolean {
            scanner.next()
            while (scanner.find('?') > 0) {
                scanner.next()
                if (scanner.next('>')) return true
            }
            return false
        }

        private fun tryComment(scanner: Scanner): Boolean {
            scanner.next()
            if (!scanner.next('-')) return false
            if (scanner.next('>') || scanner.next("->")) return true

            while (scanner.find('-') >= 0) {
                if (scanner.next("-->")) {
                    return true
                }
                scanner.next()
            }
            return false
        }

        private fun tryCdata(scanner: Scanner): Boolean {
            scanner.next()
            if (scanner.next("CDATA[")) {
                while (scanner.find(']') >= 0) {
                    if (scanner.next("]]>")) {
                        return true
                    }
                    scanner.next()
                }
            }
            return false
        }

        private fun tryDeclaration(scanner: Scanner): Boolean {
            scanner.match(ASCII_LETTER)
            if (scanner.whitespace() <= 0) return false
            if (scanner.find('>') >= 0) {
                scanner.next()
                return true
            }
            return false
        }
    }
}
