package io.heapy.kotmark.renderer.html

interface UrlSanitizer {
    fun sanitizeLinkUrl(url: String): String

    fun sanitizeImageUrl(url: String): String
}
