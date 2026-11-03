package io.heapy.kotmark.ext.front.matter

import io.heapy.kotmark.ext.front.matter.parser.FrontMatterParser
import io.heapy.kotmark.ext.front.matter.parser.FrontMatterParser.SeparatorRole
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals

class YamlFrontMatterCustomParserTest {
    @Test
    fun separatorCanBeRawContentAndCallbacksReceiveSourcePositionsOnce() {
        val ordinaryLines = mutableListOf<SourceLine>()
        val separators = mutableListOf<SourceLine>()
        val extension = YamlFrontMatterExtension.create(
            FrontMatterParser.Factory {
                object : FrontMatterParser {
                    private val content = StringBuilder()
                    private var separatorAsContent = false

                    override fun onNextLine(block: YamlFrontMatterBlock, line: SourceLine) {
                        ordinaryLines.add(line)
                        content.append(line.content).append('\n')
                    }

                    override fun onEndingSeparator(block: YamlFrontMatterBlock, separator: SourceLine): SeparatorRole {
                        separators.add(separator)
                        return if (!separatorAsContent) {
                            content.append(separator.content).append('\n')
                            separatorAsContent = true
                            SeparatorRole.CONTENT
                        } else {
                            block.appendChild(YamlFrontMatterRawContent(content.toString()))
                            SeparatorRole.BLOCK_END
                        }
                    }
                }
            },
        )
        val parser = parser {
            extensions(listOf(extension))
            includeSourceSpans = IncludeSourceSpans.BLOCKS
        }
        val document = parser.parse("---\nstring: '\n---\n'\n...\n\ngreat")

        assertEquals("string: '\n---\n'\n", YamlFrontMatterVisitor.readRawContent(document))
        assertEquals("<p>great</p>\n", htmlRenderer().render(document))
        assertEquals(listOf("string: '", "'"), ordinaryLines.map { it.content.toString() })
        assertEquals(listOf("---", "..."), separators.map { it.content.toString() })
        assertEquals(
            listOf(SourceSpan.of(1, 0, 4, 9), SourceSpan.of(3, 0, 18, 1)),
            ordinaryLines.map { it.sourceSpan },
        )
        assertEquals(
            listOf(SourceSpan.of(2, 0, 14, 3), SourceSpan.of(4, 0, 20, 3)),
            separators.map { it.sourceSpan },
        )
    }
}
