package io.heapy.kotmark.node

/**
 * The base class of all CommonMark AST nodes ([Block] and inlines).
 *
 * A node can have multiple children, and a parent (except for the root node).
 */
abstract class Node {
    private var parentField: Node? = null
    private var firstChildField: Node? = null
    private var lastChildField: Node? = null
    private var prevField: Node? = null
    private var nextField: Node? = null
    private var sourceSpans: MutableList<SourceSpan>? = null

    // Parser identity maps must not use equality or hash codes overridden by custom node types.
    internal fun identityHashCode(): Int = super.hashCode()

    abstract fun accept(visitor: Visitor)

    val next: Node? get() = nextField
    val previous: Node? get() = prevField
    val firstChild: Node? get() = firstChildField
    val lastChild: Node? get() = lastChildField
    val parent: Node? get() = parentField

    protected open fun setParent(parent: Node?) {
        parentField = parent
    }

    fun appendChild(child: Node) {
        child.unlink()
        child.setParent(this)
        val last = lastChildField
        if (last != null) {
            last.nextField = child
            child.prevField = last
        } else {
            firstChildField = child
        }
        lastChildField = child
    }

    fun prependChild(child: Node) {
        child.unlink()
        child.setParent(this)
        val first = firstChildField
        if (first != null) {
            first.prevField = child
            child.nextField = first
            firstChildField = child
        } else {
            firstChildField = child
            lastChildField = child
        }
    }

    fun unlink() {
        val prev = prevField
        val parent = parentField
        if (prev != null) {
            prev.nextField = nextField
        } else if (parent != null) {
            parent.firstChildField = nextField
        }
        val next = nextField
        if (next != null) {
            next.prevField = prevField
        } else if (parent != null) {
            parent.lastChildField = prevField
        }
        parentField = null
        nextField = null
        prevField = null
    }

    /** Inserts the [sibling] node after this node. */
    fun insertAfter(sibling: Node) {
        sibling.unlink()
        sibling.nextField = nextField
        sibling.nextField?.let { it.prevField = sibling }
        sibling.prevField = this
        nextField = sibling
        sibling.parentField = parentField
        if (sibling.nextField == null) {
            sibling.parentField?.let { it.lastChildField = sibling }
        }
    }

    /** Inserts the [sibling] node before this node. */
    fun insertBefore(sibling: Node) {
        sibling.unlink()
        sibling.prevField = prevField
        sibling.prevField?.let { it.nextField = sibling }
        sibling.nextField = this
        prevField = sibling
        sibling.parentField = parentField
        if (sibling.prevField == null) {
            sibling.parentField?.let { it.firstChildField = sibling }
        }
    }

    /** Source spans of this node if included by the parser, an empty list otherwise. */
    open fun getSourceSpans(): List<SourceSpan> = sourceSpans?.toList() ?: emptyList()

    /** Replace the current source spans with the provided list. */
    fun setSourceSpans(sourceSpans: List<SourceSpan>) {
        if (sourceSpans.isEmpty()) {
            this.sourceSpans = null
        } else {
            this.sourceSpans = sourceSpans.toMutableList()
        }
    }

    /** Add a source span to the end of the list. */
    fun addSourceSpan(sourceSpan: SourceSpan) {
        val list = sourceSpans ?: mutableListOf<SourceSpan>().also { sourceSpans = it }
        list.add(sourceSpan)
    }

    override fun toString(): String = "${this::class.simpleName}{${toStringAttributes()}}"

    protected open fun toStringAttributes(): String = ""
}
