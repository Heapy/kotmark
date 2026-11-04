package io.heapy.kotmark.docs.examples.extensions

// region toc
import io.heapy.kotmark.ext.heading.anchor.headingAnchor
import io.heapy.kotmark.ext.toc.toc
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithToc(markdown: String): String {
    val extensions = extensions {
        toc { headingLevels = 2..6 }
        headingAnchor { idPrefix = "section-" }
    }
    val document = parser { extensions(extensions) }.parse(markdown)
    return htmlRenderer { extensions(extensions) }.render(document)
}
// endregion toc
