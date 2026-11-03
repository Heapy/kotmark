# Resolving HTML destinations

URL resolution is opt-in and happens during HTML rendering. It preserves the
destinations in the AST, so the same document can be rendered against different bases
or written back to Markdown.

```kotlin
import io.heapy.kotmark.renderer.html.BaseUriUrlResolver
import io.heapy.kotmark.renderer.html.htmlRenderer

val renderer = htmlRenderer {
    urlResolver = BaseUriUrlResolver(
        baseUri = "https://example.org/docs/guide/",
        resolveLocalAnchors = false,
    )
    sanitizeUrls = true
    percentEncodeUrls = true
}
// ../images/example.png -> https://example.org/docs/images/example.png
```

The base must be an absolute hierarchical URI. A trailing slash denotes a
directory; otherwise the last path segment is treated as a file. Relative paths,
root-relative paths, protocol-relative URLs, queries and fragments follow
[RFC 3986 section 5.2](https://www.rfc-editor.org/rfc/rfc3986.html#section-5.2).
URLs with a scheme remain unchanged. Fragment-only destinations such as `#section`
stay local by default; set `resolveLocalAnchors = true` to resolve them against the
base too. An empty destination resolves to the base without its fragment.
The resolver preserves percent escapes and does not perform browser-specific
normalization or validate URLs.

Use `UrlResolver` to implement application routing or choose a different image base:

```kotlin
import io.heapy.kotmark.node.Image
import io.heapy.kotmark.renderer.html.UrlResolver

val pages = BaseUriUrlResolver("https://example.org/docs/")
val images = BaseUriUrlResolver("https://cdn.example.org/assets/")
val rendererWithRouting = htmlRenderer {
    urlResolver = UrlResolver { node, destination ->
        if (node is Image) {
            images.resolveUrl(node, destination)
        } else {
            val page = if (destination.endsWith(".md")) destination.removeSuffix(".md") + ".html" else destination
            pages.resolveUrl(node, page)
        }
    }
}
```

Core links (including references and autolinks) and images use the sequence
**resolve → optional sanitize → optional percent-encode → HTML attribute escaping**.
Raw HTML is unaffected. Custom node renderers can call `context.resolveUrl(node,
destination)` before applying their sanitization and encoding. Attribute providers
still run after URL processing and can override the final attributes.
With `urlResolver = null` (the default), existing rendering behavior is unchanged.
