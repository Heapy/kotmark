package io.heapy.kotmark.integration

import io.heapy.kotmark.ext.autolink.autolink
import io.heapy.kotmark.ext.emoji.emoji
import io.heapy.kotmark.ext.gfm.strikethrough.strikethrough
import io.heapy.kotmark.ext.gfm.tables.tables
import io.heapy.kotmark.ext.highlight.highlight
import io.heapy.kotmark.ext.math.math
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EmojiIntegrationTest {
    @Test
    fun combinesWithTablesDelimitersAndAutolinks() {
        val extensions = extensions {
            emoji()
            tables()
            highlight()
            strikethrough()
            autolink()
        }
        val parser = parser { extensions(extensions) }
        val html = htmlRenderer { extensions(extensions) }
        val markdown = markdownRenderer { extensions(extensions) }
        val document = parser.parse("| Status | Link |\n| --- | --- |\n| ==:star2:== ~~:bug:~~ | https://example.com |")
        val rendered = html.render(document)
        assertTrue("<mark>🌟</mark> <del>🐛</del>" in rendered)
        assertTrue("<a href=\"https://example.com\">https://example.com</a>" in rendered)
        assertEquals(rendered, html.render(parser.parse(markdown.render(document))))
    }

    @Test
    fun mathContentRemainsOpaqueRegardlessOfRegistrationOrder() {
        for (emojiFirst in listOf(true, false)) {
            val extensions = extensions {
                if (emojiFirst) emoji()
                math()
                if (!emojiFirst) emoji()
            }
            val document = parser { extensions(extensions) }.parse("$:heart:$ :heart:")
            assertEquals(
                "<p><span class=\"math math-inline\">:heart:</span> ❤️</p>\n",
                htmlRenderer { extensions(extensions) }.render(document),
            )
        }
    }
}
