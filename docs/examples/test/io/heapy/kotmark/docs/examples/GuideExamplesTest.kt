package io.heapy.kotmark.docs.examples

import io.heapy.kotmark.docs.examples.guides.combineExtensions
import io.heapy.kotmark.docs.examples.guides.renderWithExtensions
import io.heapy.kotmark.ext.gfm.strikethrough.strikethrough
import io.heapy.kotmark.ext.gfm.tables.TableCell
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.html.nodeRenderer
import io.heapy.kotmark.renderer.html.renderChildren
import kotlin.test.Test
import kotlin.test.assertEquals

class GuideExamplesTest {
    @Test
    fun composedExtensionsKeepCustomOverridesAndExistingSyntax() {
        val custom = HtmlRenderer.HtmlRendererExtension {
            it.nodeRenderer<TableCell> { cell ->
                val tag = if (cell.isHeader) "th" else "td"
                writer.line()
                writer.tag(tag, mapOf("class" to "custom"))
                renderChildren(cell)
                writer.tag("/$tag")
                writer.line()
            }
        }
        val extensions = combineExtensions(custom, extensions { strikethrough() })
        val document = parser { extensions(extensions) }.parse("| Title |\n| --- |\n| ~~old~~ |\n")
        assertEquals(
            "<table>\n<thead>\n<tr>\n<th class=\"custom\">Title</th>\n</tr>\n</thead>\n" +
                "<tbody>\n<tr>\n<td class=\"custom\"><del>old</del></td>\n</tr>\n</tbody>\n</table>\n",
            htmlRenderer { extensions(extensions) }.render(document),
        )
    }

    @Test
    fun sharedExtensionsParseAndRenderConfiguredSyntax() {
        assertEquals(
            "<table>\n<thead>\n<tr>\n<th>Title</th>\n</tr>\n</thead>\n" +
                "<tbody>\n<tr>\n<td><del>old</del> ~new~</td>\n</tr>\n</tbody>\n</table>\n",
            renderWithExtensions("| Title |\n| --- |\n| ~~old~~ ~new~ |\n"),
        )
    }

    @Test
    fun gettingStartedRendersMarkdown() {
        assertEquals("<p>Hello, <em>kotmark</em>.</p>\n", renderMarkdown("Hello, *kotmark*."))
    }

    @Test
    fun customBlockParsesAndEscapesContent() {
        assertEquals(
            "<aside class=\"memo\">A quiet thought &amp; a second line\n</aside>\n",
            renderMemo(":::memo\nA quiet thought & a second line\n:::"),
        )
    }

    @Test
    fun customRendererCanRenderHeadingsAndTheirChildren() {
        val document = parser().parse("# A *chapter*")
        assertEquals(
            "<h1 class=\"chapter-title\">A <em>chapter</em></h1>\n",
            chapterRenderer().render(document),
        )
    }
}
