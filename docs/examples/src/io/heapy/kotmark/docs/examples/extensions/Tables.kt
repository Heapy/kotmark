package io.heapy.kotmark.docs.examples.extensions

// region gfm-tables
import io.heapy.kotmark.ext.gfm.tables.TablesExtension
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithTables(markdown: String): String {
    val extensions = listOf(TablesExtension.create())
    val document = parser { extensions(extensions) }.parse(markdown)
    return htmlRenderer { extensions(extensions) }.render(document)
}
// endregion gfm-tables
