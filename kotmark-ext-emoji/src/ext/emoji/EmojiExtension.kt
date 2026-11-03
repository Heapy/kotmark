package io.heapy.kotmark.ext.emoji

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ExtensionsBuilder
import io.heapy.kotmark.ext.emoji.internal.EmojiInlineParser
import io.heapy.kotmark.ext.emoji.internal.emojiNodeRenderers
import io.heapy.kotmark.ext.emoji.internal.generated.gemojiAliases
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.renderer.text.TextContentRenderer
import kotlin.jvm.JvmStatic

/** Resolves case-sensitive GitHub gemoji shortcodes to Unicode, without network access. */
class EmojiExtension private constructor(private val aliases: Map<String, String>) :
    Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension,
    TextContentRenderer.TextContentRendererExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.customInlineContentParserFactory(EmojiInlineParser.Factory(aliases))
    }

    override fun extend(rendererConfiguration: HtmlRenderer.Configuration) {
        rendererConfiguration.emojiNodeRenderers()
    }

    override fun extend(rendererConfiguration: TextContentRenderer.Configuration) {
        rendererConfiguration.emojiNodeRenderers()
    }

    override fun extend(rendererConfiguration: MarkdownRenderer.Configuration) {
        rendererConfiguration.emojiNodeRenderers()
    }

    companion object {
        /**
         * Creates an extension with the bundled dictionary plus [customAliases].
         * Keys omit colons and follow [Emoji]'s shortcode rules. Values are nonempty,
         * single-line text. Custom entries replace matching built-in aliases.
         * The supplied map is copied; later mutations do not affect this extension.
         */
        @JvmStatic
        fun create(customAliases: Map<String, String> = emptyMap()): Extension {
            val additions = customAliases.toMap()
            for ([shortcode, unicode] in additions) {
                require(shortcode.isNotEmpty() && shortcode.all(::isShortcodeCharacter)) {
                    "Invalid emoji shortcode: $shortcode"
                }
                require(unicode.isNotEmpty() && '\n' !in unicode && '\r' !in unicode) {
                    "Emoji text must be nonempty and fit on one line: $shortcode"
                }
            }
            return EmojiExtension(if (additions.isEmpty()) gemojiAliases else gemojiAliases + additions)
        }
    }
}

/** Creates and appends [EmojiExtension], optionally adding or overriding shortcode mappings. */
fun ExtensionsBuilder.emoji(customAliases: Map<String, String> = emptyMap()) {
    add(EmojiExtension.create(customAliases))
}
