package io.heapy.kotmark.docs.examples.extensions

// region yaml-front-matter
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterExtension
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterVisitor
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

class Page(val metadata: Map<String, List<String>>, val html: String)

fun readPage(markdown: String): Page {
    val parser = parser { extensions(listOf(YamlFrontMatterExtension.create())) }
    val document = parser.parse(markdown)
    val visitor = YamlFrontMatterVisitor()
    document.accept(visitor)
    // The front matter is a separate node; HTML output contains only the body.
    return Page(visitor.data, htmlRenderer().render(document))
}
// endregion yaml-front-matter
