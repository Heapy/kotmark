package io.heapy.kotmark.parser.block

import io.heapy.kotmark.internal.BlockStartImpl
import kotlin.jvm.JvmStatic

abstract class BlockStart protected constructor() {
    abstract fun atIndex(newIndex: Int): BlockStart

    abstract fun atColumn(newColumn: Int): BlockStart

    /** Replaces the entire active block, including a non-paragraph block such as a block quote. */
    abstract fun replaceActiveBlockParser(): BlockStart

    /** Replaces trailing lines of an active paragraph, preserving any preceding paragraph lines. */
    abstract fun replaceParagraphLines(lines: Int): BlockStart

    companion object {
        @JvmStatic
        fun none(): BlockStart? = null

        @JvmStatic
        fun of(vararg blockParsers: BlockParser): BlockStart =
            BlockStartImpl(blockParsers)
    }
}
