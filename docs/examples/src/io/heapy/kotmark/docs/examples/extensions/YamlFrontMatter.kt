package io.heapy.kotmark.docs.examples.extensions

// region yaml-front-matter
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterVisitor
import io.heapy.kotmark.ext.front.matter.yamlFrontMatter
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

class Page(val metadata: Map<String, List<String>>, val html: String)

fun readPage(markdown: String): Page {
    val extensions = extensions { yamlFrontMatter() }
    val parser = parser { extensions(extensions) }
    val document = parser.parse(markdown)
    val visitor = YamlFrontMatterVisitor()
    document.accept(visitor)
    // The front matter is a separate node; HTML output contains only the body.
    return Page(visitor.data, htmlRenderer().render(document))
}
// endregion yaml-front-matter
