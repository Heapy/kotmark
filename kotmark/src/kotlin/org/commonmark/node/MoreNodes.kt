package org.commonmark.node

/** A fenced code block. */
class FencedCodeBlock : Block() {
    var fenceCharacter: String? = null
    var openingFenceLength: Int? = null
        set(value) {
            require(value == null || value >= 3) {
                "openingFenceLength needs to be >= 3"
            }
            checkFenceLengths(value, closingFenceLength)
            field = value
        }
    var closingFenceLength: Int? = null
        set(value) {
            require(value == null || value >= 3) {
                "closingFenceLength needs to be >= 3"
            }
            checkFenceLengths(openingFenceLength, value)
            field = value
        }
    var fenceIndent: Int = 0
    var info: String? = null
    var literal: String? = null

    @Deprecated("use fenceCharacter", ReplaceWith("fenceCharacter"))
    val fenceChar: Char
        get() = fenceCharacter?.firstOrNull() ?: '\u0000'

    @Deprecated("use fenceCharacter")
    fun setFenceChar(fenceChar: Char) {
        fenceCharacter = if (fenceChar != '\u0000') fenceChar.toString() else null
    }

    @Deprecated("use openingFenceLength", ReplaceWith("openingFenceLength ?: 0"))
    val fenceLength: Int
        get() = openingFenceLength ?: 0

    @Deprecated("use openingFenceLength")
    fun setFenceLength(fenceLength: Int) {
        openingFenceLength = if (fenceLength != 0) fenceLength else null
    }

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }

    companion object {
        private fun checkFenceLengths(openingFenceLength: Int?, closingFenceLength: Int?) {
            if (openingFenceLength != null && closingFenceLength != null) {
                require(closingFenceLength >= openingFenceLength) {
                    "fence lengths required to be: closingFenceLength >= openingFenceLength"
                }
            }
        }
    }
}

/** An indented code block. */
class IndentedCodeBlock : Block() {
    var literal: String? = null

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/** An image. */
class Image() : Node() {
    constructor(destination: String?, title: String?) : this() {
        this.destination = destination
        this.title = title
    }

    var destination: String? = null
    var title: String? = null

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }

    override fun toStringAttributes(): String = "destination=$destination, title=$title"
}

/** A link with a destination and optional title; the link text is in child nodes. */
class Link() : Node() {
    constructor(destination: String?, title: String?) : this() {
        this.destination = destination
        this.title = title
    }

    var destination: String? = null
    var title: String? = null

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }

    override fun toStringAttributes(): String = "destination=$destination, title=$title"
}

/** A link reference definition. */
class LinkReferenceDefinition() : Block() {
    constructor(label: String?, destination: String?, title: String?) : this() {
        this.label = label
        this.destination = destination
        this.title = title
    }

    var label: String? = null
    var destination: String? = null
    var title: String? = null

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/**
 * A child of a [ListBlock], containing other blocks (e.g. [Paragraph], other lists, etc).
 *
 * Note: List items should only contain block nodes. To get a tight list, set [ListBlock.isTight].
 */
class ListItem : Block() {
    var markerIndent: Int? = null
    var contentIndent: Int? = null

    /** Append a block child to this list item. */
    fun appendChildBlock(child: Block) {
        appendChild(child)
    }

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/** An ordered list. */
class OrderedList : ListBlock() {
    var markerDelimiter: String? = null
    var markerStartNumber: Int? = null

    @Deprecated("use markerStartNumber", ReplaceWith("markerStartNumber ?: 0"))
    val startNumber: Int
        get() = markerStartNumber ?: 0

    @Deprecated("use markerStartNumber")
    fun setStartNumber(startNumber: Int) {
        markerStartNumber = if (startNumber != 0) startNumber else null
    }

    @Deprecated("use markerDelimiter", ReplaceWith("markerDelimiter"))
    val delimiter: Char
        get() = markerDelimiter?.firstOrNull() ?: '\u0000'

    @Deprecated("use markerDelimiter")
    fun setDelimiter(delimiter: Char) {
        markerDelimiter = if (delimiter != '\u0000') delimiter.toString() else null
    }

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/** A paragraph block. */
class Paragraph : Block() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
