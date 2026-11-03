package io.heapy.kotmark.internal

import io.heapy.kotmark.node.HtmlBlock
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.AbstractBlockParserFactory
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.BlockStart
import io.heapy.kotmark.parser.block.MatchedBlockParser
import io.heapy.kotmark.parser.block.ParserState

class HtmlBlockParser private constructor(
    private val closingPattern: Regex?,
) : AbstractBlockParser() {
    override val block: HtmlBlock = HtmlBlock()
    private var finished: Boolean = false
    private var content: BlockContent? = BlockContent()

    override fun tryContinue(parserState: ParserState): BlockContinue? =
        if (finished || parserState.isBlank && closingPattern == null) {
            null
        } else {
            BlockContinue.AtIndex(parserState.index)
        }

    override fun addLine(line: SourceLine) {
        content?.add(line.content)
        if (closingPattern?.containsMatchIn(line.content) == true) {
            finished = true
        }
    }

    override fun closeBlock() {
        block.literal = content?.string.orEmpty()
        content = null
    }

    class Factory : AbstractBlockParserFactory() {
        override fun tryStart(
            state: ParserState,
            matchedBlockParser: MatchedBlockParser,
        ): BlockStart? {
            val nextNonSpace = state.nextNonSpaceIndex
            val line = state.line.content
            if (state.indent >= 4 || line[nextNonSpace] != '<') {
                return BlockStart.none()
            }

            val opening = line.subSequence(nextNonSpace, line.length)
            for (blockType in 1..6) {
                val patterns = BLOCK_PATTERNS[blockType]
                if (patterns.opener.containsMatchIn(opening)) {
                    return BlockStart.of(HtmlBlockParser(patterns.closer)).atIndex(state.index)
                }
            }
            if (
                matchedBlockParser.matchedBlockParser.block !is Paragraph &&
                !state.activeBlockParser.canHaveLazyContinuationLines() &&
                isCompleteTag(opening)
            ) {
                return BlockStart.of(HtmlBlockParser(null)).atIndex(state.index)
            }
            return BlockStart.none()
        }
    }

    private data class BlockPatterns(
        val opener: Regex,
        val closer: Regex?,
    )

    companion object {
        private const val TAG_NAME = "[A-Za-z][A-Za-z0-9-]*"
        private const val ATTRIBUTE_NAME = "[a-zA-Z_:][a-zA-Z0-9:._-]*"
        private const val UNQUOTED_VALUE = """[^"'=<>`\u0000-\u0020]+"""
        private const val SINGLE_QUOTED_VALUE = "'[^']*'"
        private const val DOUBLE_QUOTED_VALUE = """"[^"]*""""
        private const val ATTRIBUTE_VALUE =
            "(?:$UNQUOTED_VALUE|$SINGLE_QUOTED_VALUE|$DOUBLE_QUOTED_VALUE)"
        private const val ATTRIBUTE_VALUE_SPEC = """(?:\s*=\s*$ATTRIBUTE_VALUE)"""
        private const val ATTRIBUTE = """(?:\s+$ATTRIBUTE_NAME$ATTRIBUTE_VALUE_SPEC?)"""
        private const val CLOSE_TAG = """</$TAG_NAME\s*[>]"""

        private val IGNORE_CASE = setOf(RegexOption.IGNORE_CASE)
        private val OPEN_TAG_START = Regex("^<$TAG_NAME", IGNORE_CASE)
        private val ATTRIBUTE_PATTERN = Regex(ATTRIBUTE, IGNORE_CASE)
        private val OPEN_TAG_END = Regex("""\s*/?>\s*$""", IGNORE_CASE)
        private val CLOSE_TAG_PATTERN = Regex("""^$CLOSE_TAG\s*$""", IGNORE_CASE)

        private fun isCompleteTag(line: CharSequence): Boolean {
            if (CLOSE_TAG_PATTERN.matches(line)) return true
            var index = OPEN_TAG_START.matchAt(line, 0)?.range?.last?.plus(1) ?: return false
            // Match attributes one at a time so long tags do not recurse through a repeated
            // regex group. Keep the same greedy attribute grammar on every platform.
            while (true) {
                val attribute = ATTRIBUTE_PATTERN.matchAt(line, index) ?: break
                index = attribute.range.last + 1
            }
            val end = OPEN_TAG_END.matchAt(line, index) ?: return false
            return end.range.last + 1 == line.length
        }

        private val BLOCK_PATTERNS = listOf(
            BlockPatterns(Regex("(?!x)x"), null),
            BlockPatterns(
                Regex("""^<(?:script|pre|style|textarea)(?:\s|>|$)""", IGNORE_CASE),
                Regex("</(?:script|pre|style|textarea)>", IGNORE_CASE),
            ),
            BlockPatterns(Regex("^<!--"), Regex("-->")),
            BlockPatterns(Regex("^<[?]"), Regex("""\?>""")),
            BlockPatterns(Regex("^<![A-Z]"), Regex(">")),
            BlockPatterns(Regex("""^<!\[CDATA\["""), Regex("""\]\]>""")),
            BlockPatterns(
                Regex(
                    "^</?(?:address|article|aside|base|basefont|blockquote|body|" +
                        "caption|center|col|colgroup|dd|details|dialog|dir|div|dl|dt|" +
                        "fieldset|figcaption|figure|footer|form|frame|frameset|" +
                        "h1|h2|h3|h4|h5|h6|head|header|hr|html|iframe|legend|li|link|" +
                        "main|menu|menuitem|nav|noframes|ol|optgroup|option|p|param|" +
                        "search|section|summary|table|tbody|td|tfoot|th|thead|title|tr|track|ul)" +
                        """(?:\s|[/]?[>]|$)""",
                    IGNORE_CASE,
                ),
                null,
            ),
        )
    }
}
