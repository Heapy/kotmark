package io.heapy.kotmark.integration

import io.heapy.kotmark.ext.gfm.strikethrough.Strikethrough
import io.heapy.kotmark.ext.gfm.strikethrough.strikethrough
import io.heapy.kotmark.ext.gfm.tables.TablesExtension
import io.heapy.kotmark.ext.gfm.tables.tables
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.html.nodeRenderer
import io.heapy.kotmark.renderer.html.renderChildren
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import io.heapy.kotmark.renderer.text.textContentRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExtensionsDslTest {
    @Test
    fun configuredExtensionsCanBeSharedAcrossParsersAndRenderers() {
        lateinit var tableConfiguration: TablesExtension.Configuration
        val extensions = extensions {
            tables {
                tableConfiguration = this
                maxCells = 2
            }
            strikethrough { requireTwoTildes = true }
        }
        tableConfiguration.maxCells = 0
        val parser = parser { extensions(extensions) }
        val anotherParser = parser { extensions(extensions) }
        val html = htmlRenderer { extensions(extensions) }
        val markdown = markdownRenderer { extensions(extensions) }
        val text = textContentRenderer { extensions(extensions) }
        val source = "| Title |\n| --- |\n| ~~old~~ ~new~ |\n"
        val expected = "<table>\n<thead>\n<tr>\n<th>Title</th>\n</tr>\n</thead>\n" +
            "<tbody>\n<tr>\n<td><del>old</del> ~new~</td>\n</tr>\n</tbody>\n</table>\n"

        repeat(2) {
            val document = parser.parse(source)
            assertEquals(expected, html.render(document))
            assertEquals(expected, html.render(anotherParser.parse(markdown.render(document))))
            assertEquals("Title\n/old/ ~new~", text.render(document))
        }
        assertFailsWith<IllegalArgumentException> { parser.parse(source + "| extra |\n") }
    }

    @Test
    fun customExtensionPrecedenceFollowsListOrder() {
        val custom = HtmlRenderer.HtmlRendererExtension {
            it.nodeRenderer<Strikethrough> { node ->
                writer.tag("s")
                renderChildren(node)
                writer.tag("/s")
            }
        }
        val customFirst = extensions {
            add(custom)
            strikethrough()
        }
        val defaultFirst = extensions {
            strikethrough()
            add(custom)
        }
        val document = parser { extensions(customFirst) }.parse("~~old~~")
        assertEquals("<p><s>old</s></p>\n", htmlRenderer { extensions(customFirst) }.render(document))
        assertEquals("<p><del>old</del></p>\n", htmlRenderer { extensions(defaultFirst) }.render(document))
    }
}
