package org.commonmark.renderer.html

class DefaultUrlSanitizer(
    protocols: Collection<String> = listOf("http", "https", "mailto", "data"),
) : UrlSanitizer {
    private val protocols = protocols.toSet()

    override fun sanitizeLinkUrl(url: String): String {
        val stripped = url.trim(*HTML_SPACES)
        for (i in stripped.indices) {
            when (stripped[i]) {
                '/', '#', '?' -> break
                ':' -> {
                    if (stripped.substring(0, i).lowercase() !in protocols) {
                        return ""
                    }
                    break
                }
            }
        }
        return stripped
    }

    override fun sanitizeImageUrl(url: String): String = sanitizeLinkUrl(url)

    companion object {
        private val HTML_SPACES = charArrayOf(' ', '\t', '\n', '\u000C', '\r')
    }
}
