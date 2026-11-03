package io.heapy.kotmark.ext.emoji

import io.heapy.kotmark.ext.emoji.internal.generated.gemojiAliases
import io.heapy.kotmark.extensions
import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.html.nodeRenderer
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import io.heapy.kotmark.renderer.text.textContentRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class EmojiTest {
    private val extensions = extensions { emoji() }
    private val parser = parser { extensions(extensions) }
    private val html = htmlRenderer { extensions(extensions) }
    private val markdown = markdownRenderer { extensions(extensions) }
    private val text = textContentRenderer { extensions(extensions) }

    @Test
    fun rendersRequestedShortcodesAsUnicode() {
        val source = ":monkey: :star2: :speech_balloon: :bug: :speak_no_evil: :snail: :birthday: :heart: :fearful: :family:"
        val expected = "🐒 🌟 💬 🐛 🙊 🐌 🎂 ❤️ 😨 👪"
        val document = parser.parse(source)
        assertEquals("<p>$expected</p>\n", html.render(document))
        assertEquals(expected, text.render(document))
        assertEquals("$source\n", markdown.render(document))
    }

    @Test
    fun dictionaryPreservesAliasesAndUnicodeSequences() {
        assertEquals(1913, gemojiAliases.size)
        assertEquals("<p>👍👍👩‍💻🇺🇳❤️</p>\n", html.render(parser.parse(":+1::thumbsup::woman_technologist::united_nations::heart:")))
        // Exercise every bundled alias through the parser, including the longest aliases.
        for ([shortcode, unicode] in gemojiAliases) {
            val document = parser.parse(":$shortcode:")
            assertEquals(unicode, text.render(document), shortcode)
            assertEquals(":$shortcode:\n", markdown.render(document), shortcode)
        }
    }

    @Test
    fun unknownIncompleteAndCaseMismatchedCodesRemainText() {
        for (source in listOf(":", "::", ":missing_emoji:", ":MONKEY:", ":monkey", "monkey:", ":mon key:", ":🐒:", ":" + "a".repeat(10_000) + ":")) {
            assertEquals("<p>$source</p>\n", html.render(parser.parse(source)), source.take(80))
        }
        assertEquals("<p>:missing:🌟 and :🐒</p>\n", html.render(parser.parse(":missing::star2: and ::monkey:")))
        assertEquals("<p>before🐒after</p>\n", html.render(parser.parse("before:monkey:after")))
        assertEquals("<p>:mon\nkey:</p>\n", html.render(parser.parse(":mon\nkey:")))
    }

    @Test
    fun backslashEscapesAndEntitiesDoNotCreateEmoji() {
        assertEquals("<p>:monkey: :star2: :heart:</p>\n", html.render(parser.parse("\\:monkey: :star2\\: &#58;heart:")))
        assertEquals("<p>\\🐒</p>\n", html.render(parser.parse("\\\\:monkey:")))
    }

    @Test
    fun leavesCodeHtmlAttributesAndAutolinkDestinationsUntouched() {
        assertEquals("<p><code>:monkey:</code></p>\n", html.render(parser.parse("`:monkey:`")))
        assertEquals("<pre><code>:monkey:\n</code></pre>\n", html.render(parser.parse("```\n:monkey:\n```")))
        assertEquals("<pre><code>:monkey:\n</code></pre>\n", html.render(parser.parse("    :monkey:")))
        assertEquals("<p><span title=\":monkey:\">🐒</span></p>\n", html.render(parser.parse("<span title=\":monkey:\">:monkey:</span>")))
        assertEquals("<div>\n:monkey:\n</div>\n", html.render(parser.parse("<div>\n:monkey:\n</div>")))
        assertEquals("<p><a href=\"https://example.com/:monkey:\">https://example.com/:monkey:</a></p>\n", html.render(parser.parse("<https://example.com/:monkey:>")))
    }

    @Test
    fun worksInEmphasisLinkLabelsAndImageAltText() {
        assertEquals("<p><strong>🐒</strong> <a href=\"/:heart:\" title=\":star2:\">💬</a> <img src=\"/image\" alt=\"Gift 🎂\" /></p>\n",
            html.render(parser.parse("**:monkey:** [:speech_balloon:](/:heart: \":star2:\") ![Gift :birthday:](/image)")))
    }

    @Test
    fun markdownRoundTripPreservesEmojiAndLiteralColons() {
        for (source in listOf(
            ":monkey::star2:", ":thumbsup: :+1:", "\\:monkey: :heart:", ":monkey\\:",
            "&#58;monkey:", ":unknown:", "> :monkey:\n\n- :star2:",
            "[**:monkey:**](/:heart: \":star2:\")", "![Gift :birthday:](/image)", "`:monkey:`",
        )) {
            val first = parser.parse(source)
            val rendered = markdown.render(first)
            assertEquals(html.render(first), html.render(parser.parse(rendered)), "$source -> $rendered")
        }
    }

    @Test
    fun createsSemanticNodeWithOriginalSourceSpan() {
        val withSpans = parser {
            extensions(extensions)
            includeSourceSpans = IncludeSourceSpans.BLOCKS_AND_INLINES
        }
        val emoji = assertIs<Emoji>(withSpans.parse("😀 :heart:!").firstChild?.firstChild?.next)
        assertEquals("heart", emoji.shortcode)
        assertEquals("❤️", assertIs<Text>(emoji.firstChild).literal)
        assertNull(emoji.firstChild?.next)
        assertEquals(listOf(SourceSpan.of(0, 3, 3, 7)), emoji.getSourceSpans())
    }

    @Test
    fun customAliasesAreSnapshottedAndHtmlEscaped() {
        val aliases = mutableMapOf("party" to "🥳", "heart" to "<3 & love")
        val extensions = extensions { emoji(aliases) }
        aliases["party"] = "changed"
        aliases.clear()
        val document = parser { extensions(extensions) }.parse(":party: :heart: :monkey:")
        assertEquals("<p>🥳 &lt;3 &amp; love 🐒</p>\n", htmlRenderer { extensions(extensions) }.render(document))
        assertEquals("🥳 <3 & love 🐒", textContentRenderer { extensions(extensions) }.render(document))
        assertEquals(":party: :heart: :monkey:\n", markdownRenderer { extensions(extensions) }.render(document))
        assertEquals("<p>❤️</p>\n", html.render(parser.parse(":heart:")))
    }

    @Test
    fun rejectsInvalidCustomAliasesAndNodes() {
        for (alias in listOf("", ":heart:", "with space", "Upper", "line\nbreak", "bad*")) {
            assertFailsWith<IllegalArgumentException> { EmojiExtension.create(mapOf(alias to "🐒")) }
            assertFailsWith<IllegalArgumentException> { Emoji(alias, "🐒") }
        }
        for (unicode in listOf("", "a\nb", "a\rb")) {
            assertFailsWith<IllegalArgumentException> { EmojiExtension.create(mapOf("valid" to unicode)) }
            assertFailsWith<IllegalArgumentException> { Emoji("valid", unicode) }
        }
    }

    @Test
    fun allowsCustomRendererBeforeTheExtension() {
        val renderer = htmlRenderer {
            nodeRenderer<Emoji> { node -> writer.text(":${node.shortcode}:") }
            extensions(extensions)
        }
        assertEquals("<p>:monkey:</p>\n", renderer.render(parser.parse(":monkey:")))
    }

    @Test
    fun unicodeWorksWithoutExtensionAndShortcodesAreOptIn() {
        assertEquals("<p>🐒 :monkey:</p>\n", htmlRenderer().render(io.heapy.kotmark.parser.parser().parse("🐒 :monkey:")))
    }
}
