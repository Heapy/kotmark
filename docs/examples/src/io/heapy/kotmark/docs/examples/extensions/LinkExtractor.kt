package io.heapy.kotmark.docs.examples.extensions

// region link-extractor
import io.heapy.kotmark.autolink.LinkType
import io.heapy.kotmark.autolink.linkExtractor

fun findLinks(text: String): List<String> {
    val extractor = linkExtractor {
        linkTypes = setOf(LinkType.URL, LinkType.WWW, LinkType.EMAIL)
    }
    return extractor.extractLinks(text).map { span ->
        "${span.type}: ${text.substring(span.beginIndex, span.endIndex)}"
    }
}
// endregion link-extractor
