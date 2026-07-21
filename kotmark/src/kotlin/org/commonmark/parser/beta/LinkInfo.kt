package org.commonmark.parser.beta

import org.commonmark.node.Text

interface LinkInfo {
    fun marker(): Text?

    fun openingBracket(): Text

    fun text(): String

    fun label(): String?

    fun destination(): String?

    fun title(): String?

    fun afterTextBracket(): Position
}
