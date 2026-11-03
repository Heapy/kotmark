package io.heapy.kotmark.docs.examples

// region custom-block
import io.heapy.kotmark.Extension
import io.heapy.kotmark.node.CustomBlock
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.block.AbstractBlockParser
import io.heapy.kotmark.parser.block.AbstractBlockParserFactory
import io.heapy.kotmark.parser.block.BlockContinue
import io.heapy.kotmark.parser.block.BlockStart
import io.heapy.kotmark.parser.block.MatchedBlockParser
import io.heapy.kotmark.parser.block.ParserState
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.html.HtmlRenderer.HtmlRendererExtension
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.html.nodeRenderer

class MemoBlock : CustomBlock() {
    var literal: String = ""
}

private class MemoBlockParser : AbstractBlockParser() {
    override val block = MemoBlock()
    private val content = StringBuilder()
    private var openingLine = true

    override fun tryContinue(parserState: ParserState): BlockContinue {
        val line = parserState.line.content
            .subSequence(parserState.nextNonSpaceIndex, parserState.line.content.length)
            .toString()
            .trimEnd(' ', '\t')
        return if (line == ":::") {
            BlockContinue.Finished
        } else {
            BlockContinue.AtIndex(parserState.nextNonSpaceIndex)
        }
    }

    override fun addLine(line: SourceLine) {
        if (openingLine) {
            openingLine = false
        } else {
            content.append(line.content).append('\n')
        }
    }

    override fun closeBlock() {
        block.literal = content.toString()
    }
}

private class MemoBlockParserFactory : AbstractBlockParserFactory() {
    override fun tryStart(
        state: ParserState,
        matchedBlockParser: MatchedBlockParser,
    ): BlockStart? {
        val line = state.line.content
            .subSequence(state.nextNonSpaceIndex, state.line.content.length)
            .toString()
        if (state.indent >= 4 || line != ":::memo") return null
        return BlockStart.of(MemoBlockParser()).atIndex(state.line.content.length)
    }
}

class MemoExtension private constructor() : Parser.ParserExtension, HtmlRendererExtension {
    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.customBlockParserFactory(MemoBlockParserFactory())
    }

    override fun extend(rendererConfiguration: HtmlRenderer.Configuration) {
        rendererConfiguration.nodeRenderer<MemoBlock> { memo ->
            writer.line()
            writer.tag("aside", mapOf("class" to "memo"))
            writer.text(memo.literal)
            writer.tag("/aside")
            writer.line()
        }
    }

    companion object {
        fun create(): Extension = MemoExtension()
    }
}

fun renderMemo(markdown: String): String {
    val extensions = listOf(MemoExtension.create())
    val document = parser { extensions(extensions) }.parse(markdown)
    return htmlRenderer { extensions(extensions) }.render(document)
}
// endregion custom-block