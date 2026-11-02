package io.heapy.kotmark.renderer.html

import io.heapy.kotmark.internal.util.Escaping

class HtmlWriter(
    private val buffer: Appendable,
) {
    private var lastChar: Char = '\u0000'

    fun raw(value: String) {
        append(value)
    }

    fun text(text: String) {
        append(Escaping.escapeHtml(text))
    }

    fun tag(name: String) {
        tag(name, emptyMap())
    }

    fun tag(
        name: String,
        attributes: Map<String, String?>,
        voidElement: Boolean = false,
    ) {
        append("<")
        append(name)
        for ((key, value) in attributes) {
            append(" ")
            append(Escaping.escapeHtml(key))
            if (value != null) {
                append("=\"")
                append(Escaping.escapeHtml(value))
                append("\"")
            }
        }
        if (voidElement) {
            append(" /")
        }
        append(">")
    }

    fun line() {
        if (lastChar != '\u0000' && lastChar != '\n') {
            append("\n")
        }
    }

    private fun append(value: String) {
        buffer.append(value)
        if (value.isNotEmpty()) {
            lastChar = value.last()
        }
    }
}
