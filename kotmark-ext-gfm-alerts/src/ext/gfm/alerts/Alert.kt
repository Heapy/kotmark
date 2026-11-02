package io.heapy.kotmark.ext.gfm.alerts

import io.heapy.kotmark.node.CustomBlock

class Alert(
    val type: String,
) : CustomBlock() {
    fun hasBody(): Boolean {
        val first = firstChild
        return if (first is AlertTitle) first.next != null else first != null
    }
}
