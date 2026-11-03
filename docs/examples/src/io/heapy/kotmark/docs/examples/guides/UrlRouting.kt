package io.heapy.kotmark.docs.examples.guides

// region url-routing
import io.heapy.kotmark.node.Image
import io.heapy.kotmark.renderer.html.BaseUriUrlResolver
import io.heapy.kotmark.renderer.html.UrlResolver
import io.heapy.kotmark.renderer.html.htmlRenderer

val pages = BaseUriUrlResolver("https://example.org/docs/")
val images = BaseUriUrlResolver("https://cdn.example.org/assets/")
val routingRenderer = htmlRenderer {
    urlResolver = UrlResolver { node, destination ->
        if (node is Image) {
            images.resolveUrl(node, destination)
        } else {
            val page = if (destination.endsWith(".md")) destination.removeSuffix(".md") + ".html" else destination
            pages.resolveUrl(node, page)
        }
    }
}
// endregion url-routing
