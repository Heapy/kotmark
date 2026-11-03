package io.heapy.kotmark.parser.beta

/**
 * Opaque position within a [Scanner].
 * Positions compare by scanner-relative line and UTF-16 offset. Equality uses those coordinates,
 * even when positions come from different scanners.
 */
class Position internal constructor(
    internal val lineIndex: Int,
    internal val index: Int,
) : Comparable<Position> {
    override fun equals(other: Any?): Boolean =
        this === other || other is Position && lineIndex == other.lineIndex && index == other.index

    override fun hashCode(): Int = 31 * (31 + lineIndex) + index

    override fun compareTo(other: Position): Int {
        val lineComparison = lineIndex.compareTo(other.lineIndex)
        return if (lineComparison != 0) lineComparison else index.compareTo(other.index)
    }
}
