package io.heapy.kotmark.parser.beta

import io.heapy.kotmark.node.Text

interface LinkInfo {
    fun marker(): Text?

    fun openingBracket(): Text

    fun text(): String

    fun label(): String?

    fun destination(): String?

    fun title(): String?

    fun afterTextBracket(): Position
}
