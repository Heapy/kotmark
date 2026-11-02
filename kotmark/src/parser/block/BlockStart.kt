package io.heapy.kotmark.parser.block

import io.heapy.kotmark.internal.BlockStartImpl
import kotlin.jvm.JvmStatic

abstract class BlockStart protected constructor() {
    abstract fun atIndex(newIndex: Int): BlockStart

    abstract fun atColumn(newColumn: Int): BlockStart

    @Deprecated("Use replaceParagraphLines(Int.MAX_VALUE)")
    abstract fun replaceActiveBlockParser(): BlockStart

    abstract fun replaceParagraphLines(lines: Int): BlockStart

    companion object {
        @JvmStatic
        fun none(): BlockStart? = null

        @JvmStatic
        fun of(vararg blockParsers: BlockParser): BlockStart =
            BlockStartImpl(blockParsers)
    }
}
