package io.heapy.kotmark.internal.inline

import io.heapy.kotmark.node.HtmlInline
import io.heapy.kotmark.parser.beta.InlineContentParser
import io.heapy.kotmark.parser.beta.InlineContentParserFactory
import io.heapy.kotmark.parser.beta.InlineParserState
import io.heapy.kotmark.parser.beta.ParsedInline
import io.heapy.kotmark.parser.beta.Position
import io.heapy.kotmark.parser.beta.Scanner
import io.heapy.kotmark.text.asciiMatcher

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
        private val ASCII_LETTER = asciiMatcher {
            range('A', 'Z')
            range('a', 'z')
        }
        private val TAG_NAME_START = ASCII_LETTER
        private val TAG_NAME_CONTINUE =
            asciiMatcher(TAG_NAME_START) {
                range('0', '9')
                c('-')
            }
        private val ATTRIBUTE_START =
            asciiMatcher(ASCII_LETTER) {
                c('_')
                c(':')
            }
        private val ATTRIBUTE_CONTINUE =
            asciiMatcher(ATTRIBUTE_START) {
                range('0', '9')
                c('.')
                c('-')
            }
        private val ATTRIBUTE_VALUE_END = asciiMatcher {
            c(' ')
            c('\t')
            c('\n')
            c('\u000B')
            c('\u000C')
            c('\r')
            c('"')
            c('\'')
            c('=')
            c('<')
            c('>')
            c('`')
        }

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

            // A self-closing slash is optional.
            val _ = scanner.next('/')
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
