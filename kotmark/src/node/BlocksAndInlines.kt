package io.heapy.kotmark.node

/**
 * A block quote, e.g.:
 * ```
 * > Some quoted text
 * ```
 */
class BlockQuote : Block() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/**
 * A bullet list, e.g.:
 * ```
 * - One
 * - Two
 * - Three
 * ```
 */
class BulletList : ListBlock() {
    var marker: String? = null

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/** Inline code span. */
class Code(var literal: String? = null) : Node() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/** A block that extensions can subclass to define custom blocks. */
abstract class CustomBlock : Block() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/** A node that extensions can subclass to define custom nodes. */
abstract class CustomNode : Node() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/** A node that uses delimiters in the source form (e.g. `*bold*`). */
interface Delimited {
    val openingDelimiter: String?
    val closingDelimiter: String?
}

/** The root block of a document, containing the top-level blocks. */
class Document : Block() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/** Emphasis like `*emphasis*` or `_emphasis_`. */
class Emphasis : Node, Delimited {
    var delimiter: String? = null

    constructor()
    constructor(delimiter: String) {
        this.delimiter = delimiter
    }

    override val openingDelimiter: String? get() = delimiter
    override val closingDelimiter: String? get() = delimiter

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/** A hard line break, e.g. between two lines of text where the previous line ends in a backslash. */
class HardLineBreak : Node() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/** A heading. */
class Heading : Block() {
    var level: Int = 0

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/** HTML block. */
class HtmlBlock : Block() {
    var literal: String? = null

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/** Inline HTML element. */
class HtmlInline : Node() {
    var literal: String? = null

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/** A soft line break (as opposed to a [HardLineBreak]). */
class SoftLineBreak : Node() {
    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/** Strong emphasis like `**strong**` or `__strong__`. */
class StrongEmphasis() : Node(), Delimited {
    var delimiter: String? = null

    constructor(delimiter: String) : this() {
        this.delimiter = delimiter
    }

    override val openingDelimiter: String? get() = delimiter
    override val closingDelimiter: String? get() = delimiter

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}

/** A text node. */
class Text() : Node() {
    var literal: String? = null

    constructor(literal: String) : this() {
        this.literal = literal
    }

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }

    override fun toStringAttributes(): String = "literal=$literal"
}

/** A thematic break (horizontal rule). */
class ThematicBreak : Block() {
    /** The source literal that represents this node, if available. */
    var literal: String? = null

    override fun accept(visitor: Visitor) {
        visitor.visit(this)
    }
}
