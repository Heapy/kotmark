package io.heapy.kotmark.renderer.html

import io.heapy.kotmark.node.Node

/** Transforms a destination for HTML output without changing the Markdown AST. */
fun interface UrlResolver {
    /**
     * Returns the URL to render for [node] and its [destination], before renderer encoding.
     * The destination may already contain percent escapes from the Markdown source.
     * Core rendering calls this for links (including references and autolinks) and images.
     * The result is then sanitized and percent-encoded according to renderer configuration,
     * and HTML-escaped when written as an attribute. Return [destination] to leave it unchanged.
     *
     * Implementations should not mutate [node]. Resolver instances are shared between renders;
     * callers are responsible for any mutable state. Exceptions propagate to the caller.
     */
    fun resolveUrl(node: Node, destination: String): String
}
