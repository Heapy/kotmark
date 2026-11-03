package io.heapy.kotmark.docs.examples.guides

// region base-url
import io.heapy.kotmark.renderer.html.BaseUriUrlResolver
import io.heapy.kotmark.renderer.html.htmlRenderer

val baseUrlRenderer = htmlRenderer {
    urlResolver = BaseUriUrlResolver(
        baseUri = "https://example.org/docs/guide/",
        resolveLocalAnchors = false,
    )
    sanitizeUrls = true
    percentEncodeUrls = true
}
// ../images/example.png -> https://example.org/docs/images/example.png
// endregion base-url
