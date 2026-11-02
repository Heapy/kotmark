package io.heapy.kotmark.node

import kotlin.jvm.JvmStatic

/**
 * A list of source spans that can be added to. Takes care of merging adjacent source spans.
 */
class SourceSpans private constructor(
    private var sourceSpans: MutableList<SourceSpan>? = null,
) {
    fun getSourceSpans(): List<SourceSpan> = sourceSpans ?: emptyList()

    fun addAllFrom(nodes: Iterable<Node>) {
        for (node in nodes) {
            addAll(node.getSourceSpans())
        }
    }

    fun addAll(other: List<SourceSpan>) {
        if (other.isEmpty()) return
        val list = sourceSpans
        if (list == null) {
            sourceSpans = other.toMutableList()
            return
        }
        if (list.isEmpty()) {
            list.addAll(other)
        } else {
            val lastIndex = list.size - 1
            val a = list[lastIndex]
            val b = other[0]
            if (a.inputIndex + a.length == b.inputIndex) {
                list[lastIndex] = SourceSpan.of(a.lineIndex, a.columnIndex, a.inputIndex, a.length + b.length)
                list.addAll(other.subList(1, other.size))
            } else {
                list.addAll(other)
            }
        }
    }

    companion object {
        @JvmStatic
        fun empty(): SourceSpans = SourceSpans()
    }
}
