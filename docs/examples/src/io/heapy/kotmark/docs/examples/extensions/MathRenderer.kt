package io.heapy.kotmark.docs.examples.extensions

// region math-renderer
import io.heapy.kotmark.ext.math.InlineMath
import io.heapy.kotmark.ext.math.MathBlock
import io.heapy.kotmark.ext.math.MathExtension
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.html.nodeRenderer

fun mathAsCodeRenderer(): HtmlRenderer = htmlRenderer {
    // Register the overrides before the extension: the first renderer wins.
    nodeRenderer<InlineMath> { math ->
        writer.tag("code", mapOf("class" to "tex"))
        writer.text(math.literal)
        writer.tag("/code")
    }
    nodeRenderer<MathBlock> { math ->
        writer.tag("code", mapOf("class" to "tex"))
        writer.text(math.literal)
        writer.tag("/code")
    }
    extensions(listOf(MathExtension.create()))
}
// endregion math-renderer
