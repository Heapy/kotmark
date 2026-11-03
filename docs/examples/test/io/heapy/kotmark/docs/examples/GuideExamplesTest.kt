package io.heapy.kotmark.docs.examples

import io.heapy.kotmark.docs.examples.guides.renderWithExtensions
import io.heapy.kotmark.parser.parser
import kotlin.test.Test
import kotlin.test.assertEquals

class GuideExamplesTest {
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
