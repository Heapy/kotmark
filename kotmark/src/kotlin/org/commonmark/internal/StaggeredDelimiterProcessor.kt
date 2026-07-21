package org.commonmark.internal

import org.commonmark.parser.delimiter.DelimiterProcessor
import org.commonmark.parser.delimiter.DelimiterRun

/**
 * Dispatches delimiter runs to the registered processor with the largest applicable minimum length.
 */
internal class StaggeredDelimiterProcessor(
    private val delimiter: Char,
) : DelimiterProcessor {
    private val processors: MutableList<DelimiterProcessor> = mutableListOf()

    override val openingCharacter: Char
        get() = delimiter

    override val closingCharacter: Char
        get() = delimiter

    override var minLength: Int = 0
        private set

    fun add(processor: DelimiterProcessor) {
        val length = processor.minLength
        val insertionIndex = processors.indexOfFirst { existing ->
            when {
                length > existing.minLength -> true
                length == existing.minLength -> {
                    throw IllegalArgumentException(
                        "Cannot add two delimiter processors for char '$delimiter' and minimum length " +
                            "$length; conflicting processors: $existing, $processor",
                    )
                }

                else -> false
            }
        }

        if (insertionIndex >= 0) {
            processors.add(insertionIndex, processor)
        } else {
            processors.add(processor)
            minLength = length
        }
    }

    override fun process(openingRun: DelimiterRun, closingRun: DelimiterRun): Int =
        findProcessor(openingRun.length()).process(openingRun, closingRun)

    private fun findProcessor(length: Int): DelimiterProcessor =
        processors.firstOrNull { it.minLength <= length } ?: processors.first()
}
