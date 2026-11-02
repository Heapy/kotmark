package io.heapy.kotmark.ext.gfm.tables

import io.heapy.kotmark.Extension
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.renderer.text.LineBreakRendering
import io.heapy.kotmark.renderer.text.TextContentRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TablesTest {
    private val extensions: List<Extension> = listOf(TablesExtension.create())
    private val parser = Parser.builder().extensions(extensions).build()
    private val html = HtmlRenderer.builder().extensions(extensions).build()

    @Test
    fun tableSyntaxAndAlignment() {
        assertEquals("<p>Abc|Def</p>\n", render("Abc|Def"))
        assertEquals("<p>Abc|Def\n---||---</p>\n", render("Abc|Def\n---||---"))
        assertEquals("<p>Abc|Def|Ghi\n---|---\n1|2|3</p>\n", render("Abc|Def|Ghi\n---|---\n1|2|3"))
        assertEquals(
            tableHtml(
                """
                    |<th align="left">Abc</th>
                    |<th align="center">Def</th>
                    |
                """.trimMargin(),
                """
                    |<td align="left">1</td>
                    |<td align="center">2</td>
                    |
                """.trimMargin(),
            ),
            render("Abc|Def\n:---|:---:\n1|2"),
        )
    }

    @Test
    fun escapedPipesPaddingAndColumnCounts() {
        assertEquals(
            tableHtml(
                """
                    |<th>Abc</th>
                    |<th>Def</th>
                    |
                """.trimMargin(),
                """
                    |<td>1|2</td>
                    |<td>20</td>
                    |
                """.trimMargin(),
            ),
            render(" Abc  | Def \n --- | --- \n 1\\|2 | 20 "),
        )
        assertEquals(
            tableHtml(
                """
                    |<th>Abc</th>
                    |<th>Def</th>
                    |<th>Ghi</th>
                    |
                """.trimMargin(),
                """
                    |<td>1</td>
                    |<td>2</td>
                    |<td></td>
                    |
                """.trimMargin(),
            ),
            render("Abc|Def|Ghi\n---|---|---\n1|2"),
        )
        assertEquals(
            tableHtml(
                """
                    |<th>Abc</th>
                    |<th>Def</th>
                    |
                """.trimMargin(),
                """
                    |<td>1</td>
                    |<td>2</td>
                    |
                """.trimMargin(),
            ),
            render("Abc|Def\n---|---\n1|2|3"),
        )
    }

    @Test
    fun danglingPipeEndsTable() {
        assertEquals(
            tableHtml(
                """
                    |<th>Abc</th>
                    |<th>Def</th>
                    |
                """.trimMargin(),
                """
                    |<td>1</td>
                    |<td>2</td>
                    |
                """.trimMargin(),
            ) + "<p>|</p>\n",
            render("Abc|Def\n---|---\n1|2\n  |  "),
        )
    }

    @Test
    fun markdownRoundTripsCanonicalTables() {
        val markdown = MarkdownRenderer.builder().extensions(extensions).build()
        for (
            input in
                listOf(
                    "|Abc|\n|---|\n",
                    """
                        ||Abc|Def|
                        ||---|---|
                        ||1|2|
                        |
                    """.trimMargin(),
                    """
                        ||Abc|Def|
                        ||:---|---:|
                        ||1|2|
                        |
                    """.trimMargin(),
                    """
                        |> |Abc|Def|
                        |> |---|---|
                        |> |1|2|
                        |
                    """.trimMargin(),
                    """
                        ||Abc|Def|
                        ||---|---|
                        ||Pipe in|text \||
                        |
                    """.trimMargin(),
                    """
                        ||Abc|Def|
                        ||---|---|
                        ||Pipe in|code `\|`|
                        |
                    """.trimMargin(),
                )
        ) {
            assertEquals(input, markdown.render(parser.parse(input)))
        }
        assertEquals(
            "\\|Abc\\|\n\\|---\\|\n",
            markdown.render(parser.parse("\\|Abc\\|\n\\|---\\|\n")),
        )
    }

    @Test
    fun textRenderingModes() {
        val source = """
            |Foo
            |
            |Abc|Def
            |---|---
            |1|2
            |
            |Bar
        """.trimMargin()
        assertText(source, LineBreakRendering.COMPACT, "Foo\nAbc| Def\n1| 2\nBar")
        assertText(
            source,
            LineBreakRendering.SEPARATE_BLOCKS,
            """
                |Foo
                |
                |Abc| Def
                |1| 2
                |
                |Bar
            """.trimMargin(),
        )
        assertText(source, LineBreakRendering.STRIP, "Foo Abc| Def 1| 2 Bar")
    }

    @Test
    fun cellMetadataAndSourceSpans() {
        val spansParser =
            Parser.builder()
                .extensions(extensions)
                .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
                .build()
        val table =
            assertIs<TableBlock>(
                spansParser.parse("Abc|Def\n-----|:---:\n|1|2\n").firstChild,
            )
        assertEquals(
            listOf(
                SourceSpan.of(0, 0, 0, 7),
                SourceSpan.of(1, 0, 8, 11),
                SourceSpan.of(2, 0, 20, 4),
            ),
            table.getSourceSpans(),
        )
        val head = assertIs<TableHead>(table.firstChild)
        val headerRow = assertIs<TableRow>(head.firstChild)
        val firstHeader = assertIs<TableCell>(headerRow.firstChild)
        val secondHeader = assertIs<TableCell>(headerRow.lastChild)
        assertEquals(5, firstHeader.width)
        assertEquals(null, firstHeader.alignment)
        assertEquals(5, secondHeader.width)
        assertEquals(TableCell.Alignment.CENTER, secondHeader.alignment)
        assertEquals(listOf(SourceSpan.of(0, 0, 0, 3)), firstHeader.getSourceSpans())
        assertEquals(listOf(SourceSpan.of(0, 4, 4, 3)), secondHeader.getSourceSpans())
    }

    @Test
    fun attributeProvidersSeeAllTableNodes() {
        val renderer =
            HtmlRenderer.builder()
                .attributeProviderFactory {
                    { node, _, attributes ->
                        val value =
                            when (node) {
                                is TableBlock -> "block"
                                is TableHead -> "head"
                                is TableBody -> "body"
                                is TableRow -> "row"
                                is TableCell -> "cell"
                                else -> null
                            }
                        value?.let { attributes["test"] = it }
                    }
                }
                .extensions(extensions)
                .build()
        assertEquals(
            """
                |<table test="block">
                |<thead test="head">
                |<tr test="row">
                |<th test="cell">A</th>
                |</tr>
                |</thead>
                |<tbody test="body">
                |<tr test="row">
                |<td test="cell">1</td>
                |</tr>
                |</tbody>
                |</table>
                |
            """.trimMargin(),
            renderer.render(parser.parse("A|\n-|\n1|")),
        )
    }

    private fun assertText(source: String, mode: LineBreakRendering, expected: String) {
        val renderer =
            TextContentRenderer.builder()
                .extensions(extensions)
                .lineBreakRendering(mode)
                .build()
        assertEquals(expected, renderer.render(parser.parse(source)))
    }

    private fun render(source: String): String = html.render(parser.parse(source))

    private fun tableHtml(headerCells: String, bodyCells: String? = null): String =
        """
            |<table>
            |<thead>
            |<tr>
            |
        """.trimMargin() +
            headerCells +
            "</tr>\n" +
            "</thead>\n" +
            if (bodyCells != null) {
                "<tbody>\n" +
                    "<tr>\n" +
                    bodyCells +
                    """
                        |</tr>
                        |</tbody>
                        |</table>
                        |
                    """.trimMargin()
            } else {
                "</table>\n"
            }
}
