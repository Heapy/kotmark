package io.heapy.kotmark.internal.util

object Parsing {
    const val CODE_BLOCK_INDENT: Int = 4

    fun columnsToNextTabStop(column: Int): Int = 4 - (column % 4)
}
