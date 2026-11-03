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
    // A scan reaching the end proves that later scans in the same inline snippet cannot find
    // the terminator either. Remembering that avoids quadratic rescanning of unclosed constructs.
    // A parser is created for each inline snippet, so these flags do not need to be reset.
    private var noProcessingInstructionEnd = false
    private var noCommentEnd = false
    private var noCdataEnd = false
    private var noDeclarationEnd = false

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

    private fun tryProcessingInstruction(scanner: Scanner): Boolean {
        if (noProcessingInstructionEnd) return false
        scanner.next()
        var found = scanner.find('?')
        while (found > 0) {
            scanner.next()
            if (scanner.next('>')) return true
            found = scanner.find('?')
        }
        // Finding `?` at the current position is an early failure, not the end of the input.
        noProcessingInstructionEnd = found < 0
        return false
    }

    private fun tryComment(scanner: Scanner): Boolean {
        // Both short comments, `<!-->` and `<!--->`, also contain `-->`.
        if (noCommentEnd) return false
        scanner.next()
        if (!scanner.next('-')) return false
        if (scanner.next('>') || scanner.next("->")) return true

        while (scanner.find('-') >= 0) {
            if (scanner.next("-->")) {
                return true
            }
            scanner.next()
        }
        noCommentEnd = true
        return false
    }

    private fun tryCdata(scanner: Scanner): Boolean {
        if (noCdataEnd) return false
        scanner.next()
        if (scanner.next("CDATA[")) {
            while (scanner.find(']') >= 0) {
                if (scanner.next("]]>")) {
                    return true
                }
                scanner.next()
            }
            noCdataEnd = true
        }
        return false
    }

    private fun tryDeclaration(scanner: Scanner): Boolean {
        if (noDeclarationEnd) return false
        scanner.match(ASCII_LETTER)
        if (scanner.whitespace() <= 0) return false
        if (scanner.find('>') >= 0) {
            scanner.next()
            return true
        }
        noDeclarationEnd = true
        return false
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
    }
}
