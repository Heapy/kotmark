package io.heapy.kotmark.docs.examples.guides

// region extensions-list
import io.heapy.kotmark.ext.gfm.strikethrough.strikethrough
import io.heapy.kotmark.ext.gfm.tables.tables
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithExtensions(markdown: String): String {
    val extensions = extensions {
        tables { maxCells = 100_000 }
        strikethrough { requireTwoTildes = true }
    }
    val parser = parser { extensions(extensions) }
    val renderer = htmlRenderer { extensions(extensions) }
    return renderer.render(parser.parse(markdown))
}
// endregion extensions-list
