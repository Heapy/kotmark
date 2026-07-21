package org.commonmark.parser.block

import org.commonmark.internal.BlockContinueImpl
import kotlin.jvm.JvmStatic

open class BlockContinue protected constructor() {
    companion object {
        @JvmStatic
        fun none(): BlockContinue? = null

        @JvmStatic
        fun atIndex(newIndex: Int): BlockContinue =
            BlockContinueImpl(newIndex, -1, false)

        @JvmStatic
        fun atColumn(newColumn: Int): BlockContinue =
            BlockContinueImpl(-1, newColumn, false)

        @JvmStatic
        fun finished(): BlockContinue =
            BlockContinueImpl(-1, -1, true)
    }
}
