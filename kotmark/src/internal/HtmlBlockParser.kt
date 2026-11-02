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
            BlockContinue.none()
        } else {
            BlockContinue.atIndex(parserState.index)
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

            for (blockType in 1..7) {
                if (
                    blockType == 7 &&
                    (
                        matchedBlockParser.matchedBlockParser.block is Paragraph ||
                            state.activeBlockParser.canHaveLazyContinuationLines()
                    )
                ) {
                    continue
                }

                val patterns = BLOCK_PATTERNS[blockType]
                if (patterns.opener.containsMatchIn(line.subSequence(nextNonSpace, line.length))) {
                    return BlockStart.of(HtmlBlockParser(patterns.closer)).atIndex(state.index)
                }
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
        private const val OPEN_TAG = """<$TAG_NAME$ATTRIBUTE*\s*/?>"""
        private const val CLOSE_TAG = """</$TAG_NAME\s*[>]"""

        private val IGNORE_CASE = setOf(RegexOption.IGNORE_CASE)
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
            BlockPatterns(
                Regex("""^(?:$OPEN_TAG|$CLOSE_TAG)\s*$""", IGNORE_CASE),
                null,
            ),
        )
    }
}
