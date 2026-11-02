package io.heapy.kotmark.ext.image.attributes.internal

import io.heapy.kotmark.ext.image.attributes.ImageAttributes
import io.heapy.kotmark.node.Image
import io.heapy.kotmark.node.Nodes
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.parser.delimiter.DelimiterProcessor
import io.heapy.kotmark.parser.delimiter.DelimiterRun

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
        private val WHITESPACE = Regex("""\s+""")
    }
}
