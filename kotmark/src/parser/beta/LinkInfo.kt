package io.heapy.kotmark.parser.beta

import io.heapy.kotmark.node.Text

interface LinkInfo {
    fun marker(): Text?

    fun openingBracket(): Text

    /**
     * The text between the first brackets, such as `foo` in `[foo][bar]`.
     * Call during link processing with the scanner passed to [LinkProcessor.process].
     * Extracting the text does not change the scanner's position.
     */
    fun text(scanner: Scanner): String

    fun label(): String?

    fun destination(): String?

    fun title(): String?

    fun afterTextBracket(): Position
}
