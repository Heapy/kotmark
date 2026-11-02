package io.heapy.kotmark.parser.block

import kotlin.jvm.JvmStatic

/**
 * The outcome of continuing an open block on the current line.
 *
 * Return `null` from [BlockParser.tryContinue] when the block does not continue.
 * Positions are absolute within the current line, not offsets from the parser's current position.
 */
sealed class BlockContinue {
    /** Continue at the given zero-based UTF-16 character index. */
    data class AtIndex(val newIndex: Int) : BlockContinue() {
        init {
            require(newIndex >= 0) { "newIndex must be >= 0" }
        }
    }

    /** Continue at the given zero-based visual column, accounting for expanded tabs. */
    data class AtColumn(val newColumn: Int) : BlockContinue() {
        init {
            require(newColumn >= 0) { "newColumn must be >= 0" }
        }
    }

    /** Close this block and its open children, consuming the current line as a closing delimiter. */
    data object Finished : BlockContinue()

    companion object {
        /** The block does not continue; the current line remains available for further parsing. */
        @JvmStatic
        fun none(): BlockContinue? = null

        @JvmStatic
        fun atIndex(newIndex: Int): BlockContinue =
            AtIndex(newIndex)

        @JvmStatic
        fun atColumn(newColumn: Int): BlockContinue =
            AtColumn(newColumn)

        @JvmStatic
        fun finished(): BlockContinue =
            Finished
    }
}
