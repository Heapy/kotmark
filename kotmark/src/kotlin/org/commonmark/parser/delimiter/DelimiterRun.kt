package org.commonmark.parser.delimiter

import org.commonmark.node.Text

interface DelimiterRun {
    fun canOpen(): Boolean

    fun canClose(): Boolean

    fun length(): Int

    fun originalLength(): Int

    val opener: Text

    val closer: Text

    fun getOpeners(length: Int): Iterable<Text>

    fun getClosers(length: Int): Iterable<Text>
}
