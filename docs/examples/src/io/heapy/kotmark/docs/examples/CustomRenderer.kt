package io.heapy.kotmark.docs.examples

// region custom-renderer
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.html.nodeRenderer
import io.heapy.kotmark.renderer.html.renderChildren

fun chapterRenderer(): HtmlRenderer = htmlRenderer {
    nodeRenderer<Heading> { heading ->
        val tag = "h${heading.level}"
        writer.line()
        writer.tag(tag, extendAttributes(heading, tag, mapOf("class" to "chapter-title")))
        renderChildren(heading)
        writer.tag("/$tag")
        writer.line()
    }
}
// endregion custom-renderer
