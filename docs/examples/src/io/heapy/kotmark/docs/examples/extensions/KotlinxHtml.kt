package io.heapy.kotmark.docs.examples.extensions

// region kotlinx-html
import io.heapy.kotmark.ext.gfm.tables.TablesExtension
import io.heapy.kotmark.ext.math.MathExtension
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.kotlinxhtml.adapters.mathAdapter
import io.heapy.kotmark.renderer.kotlinxhtml.adapters.tablesAdapter
import io.heapy.kotmark.renderer.kotlinxhtml.kotlinxHtmlRenderer
import kotlinx.html.article
import kotlinx.html.h1
import kotlinx.html.stream.createHTML

fun renderArticle(title: String, markdown: String): String {
    val parser = parser { extensions(listOf(MathExtension.create(), TablesExtension.create())) }
    // Each syntax extension needs its matching adapter on the renderer side.
    val renderer = kotlinxHtmlRenderer { adapters(mathAdapter(), tablesAdapter()) }
    val document = parser.parse(markdown)
    return createHTML(prettyPrint = false).article {
        h1 { +title }
        renderer.render(document, this)
    }
}
// endregion kotlinx-html
