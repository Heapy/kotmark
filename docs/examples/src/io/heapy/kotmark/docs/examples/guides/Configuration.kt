package io.heapy.kotmark.docs.examples.guides

// region configuration
import io.heapy.kotmark.ext.gfm.strikethrough.strikethrough
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

val extensions = extensions {
    strikethrough { requireTwoTildes = true }
}
val configuredParser = parser {
    includeSourceSpans = IncludeSourceSpans.BLOCKS_AND_INLINES
    extensions(extensions)
}
val configuredRenderer = htmlRenderer {
    escapeHtml = true
    softbreak = "<br />\n"
    extensions(extensions)
}

fun renderConfigured(markdown: String): String =
    configuredRenderer.render(configuredParser.parse(markdown))
// endregion configuration
