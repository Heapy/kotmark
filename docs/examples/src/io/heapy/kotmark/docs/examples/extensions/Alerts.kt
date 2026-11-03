package io.heapy.kotmark.docs.examples.extensions

// region gfm-alerts
import io.heapy.kotmark.ext.gfm.alerts.alerts
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

fun renderWithAlerts(markdown: String): String {
    val extensions = extensions {
        alerts {
            // NOTE, TIP, IMPORTANT, WARNING, and CAUTION are allowed by default.
            addCustomType("INFO", "Information")
        }
    }
    val document = parser { extensions(extensions) }.parse(markdown)
    return htmlRenderer { extensions(extensions) }.render(document)
}
// endregion gfm-alerts
