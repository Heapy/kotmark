package org.commonmark.internal

import org.commonmark.node.Text
import org.commonmark.parser.delimiter.DelimiterRun

class Delimiter(
    val characters: MutableList<Text>,
    val delimiterChar: Char,
    private val canOpenValue: Boolean,
    private val canCloseValue: Boolean,
    var previous: Delimiter?,
) : DelimiterRun {
    private val originalLengthValue: Int = characters.size

    var next: Delimiter? = null

    override fun canOpen(): Boolean = canOpenValue

    override fun canClose(): Boolean = canCloseValue

    override fun length(): Int = characters.size

    override fun originalLength(): Int = originalLengthValue

    override val opener: Text
        get() = characters.last()

    override val closer: Text
        get() = characters.first()

    override fun getOpeners(length: Int): Iterable<Text> {
        require(length in 1..length()) {
            "length must be between 1 and ${length()}, was $length"
        }
        return characters.subList(characters.size - length, characters.size)
    }

    override fun getClosers(length: Int): Iterable<Text> {
        require(length in 1..length()) {
            "length must be between 1 and ${length()}, was $length"
        }
        return characters.subList(0, length)
    }
}
