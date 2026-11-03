package io.heapy.kotmark.docs.examples.extensions

// region math-renderer
import io.heapy.kotmark.ext.math.InlineMath
import io.heapy.kotmark.ext.math.MathBlock
import io.heapy.kotmark.ext.math.math
import io.heapy.kotmark.extensions
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.html.nodeRenderer

fun mathAsCodeRenderer(): HtmlRenderer {
    val extensions = extensions { math() }
    return htmlRenderer {
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
        extensions(extensions)
    }
}
// endregion math-renderer
