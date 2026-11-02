package io.heapy.kotmark.ext.gfm.tables

import io.heapy.kotmark.node.CustomNode

class TableCell : CustomNode() {
    var isHeader: Boolean = false
    var alignment: Alignment? = null
    var width: Int = 0

    enum class Alignment {
        LEFT,
        CENTER,
        RIGHT,
    }
}
