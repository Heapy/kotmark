package io.heapy.kotmark.renderer.html

import io.heapy.kotmark.node.Node

/**
 * Resolves relative destinations using the path, query and fragment rules of
 * [RFC 3986 section 5.2](https://www.rfc-editor.org/rfc/rfc3986.html#section-5.2).
 * Destinations that already have a scheme are returned unchanged.
 *
 * [baseUri] must be absolute and hierarchical, for example `https://example.org/docs/`.
 * A trailing slash denotes a directory; without it the last path segment is a file.
 * Empty destinations resolve to the base without its fragment. Query-only destinations
 * retain the base path. Local anchors (`#section`, including `#`) are preserved unless
 * [resolveLocalAnchors] is true, for both links and images.
 *
 * This resolver neither validates nor sanitizes destination URLs. It preserves percent
 * escapes and leaves spaces and Unicode for the renderer's optional percent encoding.
 * It uses URI syntax, not browser URL normalization (for example, backslashes are not slashes).
 *
 * @throws IllegalArgumentException if [baseUri] has no scheme or hierarchical path/authority.
 */
class BaseUriUrlResolver(
    baseUri: String,
    private val resolveLocalAnchors: Boolean = false,
) : UrlResolver {
    private val base = UriReference.parse(baseUri)

    init {
        require(base.scheme != null && (base.authority != null || base.path.startsWith('/'))) {
            "baseUri must be an absolute hierarchical URI"
        }
    }

    override fun resolveUrl(node: Node, destination: String): String {
        if (!resolveLocalAnchors && destination.startsWith('#')) return destination
        val reference = UriReference.parse(destination)
        if (reference.scheme != null) return destination

        val target = when {
            reference.authority != null -> reference.copy(
                scheme = base.scheme,
                path = removeDotSegments(reference.path),
            )
            reference.path.isEmpty() -> reference.copy(
                scheme = base.scheme,
                authority = base.authority,
                path = base.path,
                query = reference.query ?: base.query,
            )
            else -> reference.copy(
                scheme = base.scheme,
                authority = base.authority,
                path = removeDotSegments(
                    if (reference.path.startsWith('/')) reference.path else mergePath(reference.path),
                ),
            )
        }
        return target.compose()
    }

    private fun mergePath(path: String): String =
        if (base.authority != null && base.path.isEmpty()) {
            "/$path"
        } else {
            base.path.substring(0, base.path.lastIndexOf('/') + 1) + path
        }
}

private data class UriReference(
    val scheme: String?,
    val authority: String?,
    val path: String,
    val query: String?,
    val fragment: String?,
) {
    fun compose(): String = buildString {
        scheme?.let { append(it).append(':') }
        authority?.let { append("//").append(it) }
        append(path)
        query?.let { append('?').append(it) }
        fragment?.let { append('#').append(it) }
    }

    companion object {
        fun parse(uri: String): UriReference {
            val fragmentIndex = uri.indexOf('#')
            val fragment = if (fragmentIndex >= 0) uri.substring(fragmentIndex + 1) else null
            val withoutFragment = if (fragmentIndex >= 0) uri.substring(0, fragmentIndex) else uri
            val queryIndex = withoutFragment.indexOf('?')
            val query = if (queryIndex >= 0) withoutFragment.substring(queryIndex + 1) else null
            val hierarchical = if (queryIndex >= 0) withoutFragment.substring(0, queryIndex) else withoutFragment
            val colon = hierarchical.indexOf(':')
            val scheme = if (colon > 0 && hierarchical[0].isSchemeLetter() &&
                hierarchical.substring(1, colon).all { it.isSchemeLetter() || it in '0'..'9' || it in "+-." }
            ) {
                hierarchical.substring(0, colon)
            } else {
                null
            }
            val start = if (scheme != null) colon + 1 else 0
            val authorityEnd = if (hierarchical.startsWith("//", start)) {
                hierarchical.indexOf('/', start + 2).let { if (it < 0) hierarchical.length else it }
            } else {
                -1
            }
            return UriReference(
                scheme,
                if (authorityEnd >= 0) hierarchical.substring(start + 2, authorityEnd) else null,
                hierarchical.substring(if (authorityEnd >= 0) authorityEnd else start),
                query,
                fragment,
            )
        }

        private fun Char.isSchemeLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'
    }
}

// Scan with a cursor rather than repeatedly copying the remaining path.
private fun removeDotSegments(path: String): String = buildString {
    var index = 0
    while (index < path.length) {
        when {
            path.startsWith("../", index) -> index += 3
            path.startsWith("./", index) -> index += 2
            path.startsWith("/./", index) -> index += 2
            path.startsWith("/.", index) && index + 2 == path.length -> {
                append('/')
                index += 2
            }
            path.startsWith("/../", index) -> {
                deleteRange(lastIndexOf('/').coerceAtLeast(0), length)
                index += 3
            }
            path.startsWith("/..", index) && index + 3 == path.length -> {
                deleteRange(lastIndexOf('/').coerceAtLeast(0), length)
                append('/')
                index += 3
            }
            (path[index] == '.' && index + 1 == path.length) ||
                (path.startsWith("..", index) && index + 2 == path.length) -> index = path.length
            else -> {
                val nextSlash = path.indexOf('/', index + if (path[index] == '/') 1 else 0)
                val end = if (nextSlash < 0) path.length else nextSlash
                append(path, index, end)
                index = end
            }
        }
    }
}
