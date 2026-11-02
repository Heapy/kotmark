package org.commonmark.ext.image.attributes.internal

import org.commonmark.ext.image.attributes.ImageAttributes
import org.commonmark.node.Image
import org.commonmark.node.Nodes
import org.commonmark.node.Text
import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun

class ImageAttributesDelimiterProcessor : DelimiterProcessor {
    override val openingCharacter: Char = '{'
    override val closingCharacter: Char = '}'
    override val minLength: Int = 1

    override fun process(
        openingRun: DelimiterRun,
        closingRun: DelimiterRun,
    ): Int {
        if (openingRun.length() != 1) {
            return 0
        }
        val opener = openingRun.opener
        val image = opener.previous as? Image ?: return 0
        val toUnlink = mutableListOf<Text>()
        val content = StringBuilder()
        for (node in Nodes.between(opener, closingRun.closer)) {
            if (node !is Text) {
                return 0
            }
            content.append(node.literal)
            toUnlink.add(node)
        }

        // Java String.split keeps the empty input as one (invalid) attribute.
        if (content.isEmpty()) return 0
        val attributes = linkedMapOf<String, String>()
        for (candidate in content.toString().split(WHITESPACE).dropLastWhile(String::isEmpty)) {
            val parts = candidate.split('=').dropLastWhile(String::isEmpty)
            if (parts.size <= 1 || parts[0].lowercase() !in SUPPORTED_ATTRIBUTES) {
                return 0
            }
            attributes[parts[0]] = parts[1]
        }
        toUnlink.forEach(Text::unlink)
        if (attributes.isNotEmpty()) {
            image.appendChild(ImageAttributes(attributes))
        }
        return 1
    }

    companion object {
        private val SUPPORTED_ATTRIBUTES = setOf("width", "height")
        private val WHITESPACE = Regex("\\s+")
    }
}
