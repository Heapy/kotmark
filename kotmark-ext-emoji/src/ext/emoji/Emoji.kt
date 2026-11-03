package io.heapy.kotmark.ext.emoji

import io.heapy.kotmark.node.CustomNode
import io.heapy.kotmark.node.Text

/**
 * An emoji written as `:shortcode:`. Its Unicode text is a [Text] child so visitors
 * that collect text (including image alt text) can read it without knowing this node type.
 * Markdown rendering preserves [shortcode]; HTML and plain text render the children.
 */
class Emoji(val shortcode: String, unicode: String) : CustomNode() {
    init {
        require(shortcode.isNotEmpty() && shortcode.all(::isShortcodeCharacter)) {
            "Emoji shortcode must contain only lowercase ASCII letters, digits, '_', '+' or '-'"
        }
        require(unicode.isNotEmpty() && '\n' !in unicode && '\r' !in unicode) {
            "Emoji text must be nonempty and fit on one line"
        }
        appendChild(Text(unicode))
    }
}

internal fun isShortcodeCharacter(character: Char): Boolean =
    character in 'a'..'z' || character in '0'..'9' || character == '_' || character == '+' || character == '-'
