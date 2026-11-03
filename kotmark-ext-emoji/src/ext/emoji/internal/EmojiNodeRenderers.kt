package io.heapy.kotmark.ext.emoji.internal

import io.heapy.kotmark.ext.emoji.Emoji
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.html.nodeRenderer
import io.heapy.kotmark.renderer.html.renderChildren
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.renderer.markdown.nodeRenderer
import io.heapy.kotmark.renderer.text.TextContentRenderer
import io.heapy.kotmark.renderer.text.nodeRenderer
import io.heapy.kotmark.renderer.text.renderChildren

internal fun HtmlRenderer.Configuration.emojiNodeRenderers() {
    nodeRenderer<Emoji> { node -> renderChildren(node) }
}

internal fun TextContentRenderer.Configuration.emojiNodeRenderers() {
    nodeRenderer<Emoji> { node -> renderChildren(node) }
}

internal fun MarkdownRenderer.Configuration.emojiNodeRenderers() {
    // Escape colons in plain Text nodes so literal/escaped shortcodes stay literal on reparse.
    nodeRenderer<Emoji>(specialCharacters = setOf(':')) { node ->
        writer.raw(":${node.shortcode}:")
    }
}
