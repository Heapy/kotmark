package org.commonmark.ext.gfm.alerts

import org.commonmark.node.CustomBlock

class Alert(
    val type: String,
) : CustomBlock() {
    fun hasBody(): Boolean {
        val first = firstChild
        return if (first is AlertTitle) first.next != null else first != null
    }
}
