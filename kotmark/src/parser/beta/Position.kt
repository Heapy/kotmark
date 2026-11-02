package io.heapy.kotmark.parser.beta

/**
 * Opaque position within a [Scanner].
 */
class Position internal constructor(
    internal val lineIndex: Int,
    internal val index: Int,
)
