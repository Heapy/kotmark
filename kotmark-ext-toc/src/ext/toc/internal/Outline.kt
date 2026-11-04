package io.heapy.kotmark.ext.toc.internal

import io.heapy.kotmark.node.BlockQuote
import io.heapy.kotmark.node.BulletList
import io.heapy.kotmark.node.Code
import io.heapy.kotmark.node.Document
import io.heapy.kotmark.node.HardLineBreak
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.Link
import io.heapy.kotmark.node.ListBlock
import io.heapy.kotmark.node.ListItem
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.node.Paragraph
import io.heapy.kotmark.node.SoftLineBreak
import io.heapy.kotmark.node.Text

/** Walk standard block containers iteratively. Custom blocks may hide or defer their children. */
internal fun documentNodes(root: Node): Sequence<Node> = sequence {
    val pending = ArrayDeque<Node>()
    pending.add(root)
    while (pending.isNotEmpty()) {
        val node = pending.removeLast()
        yield(node)
        if (node is Document || node is BlockQuote || node is ListBlock || node is ListItem) {
            var child = node.lastChild
            while (child != null) {
                pending.add(child)
                child = child.previous
            }
        }
    }
}

internal fun headingText(heading: Heading): String = buildString {
    val pending = ArrayDeque<Node>()
    pending.add(heading)
    while (pending.isNotEmpty()) {
        val node = pending.removeLast()
        when (node) {
            is Text -> append(node.literal)
            is Code -> append(node.literal)
            is SoftLineBreak, is HardLineBreak -> append(' ')
        }
        var child = node.lastChild
        while (child != null) {
            pending.add(child)
            child = child.previous
        }
    }
}.trim()

internal data class Entry(val level: Int, val label: String, val destination: String? = null)

/** The nearest preceding lower-level entry is the parent; skipped levels create no empty items. */
internal fun outline(entries: List<Entry>): BulletList {
    val root = BulletList().apply { isTight = true; marker = "-" }
    val ancestors = mutableListOf<Pair<Int, ListItem>>()
    for (entry in entries) {
        while (ancestors.isNotEmpty() && ancestors.last().first >= entry.level) ancestors.removeAt(ancestors.lastIndex)
        val parent = ancestors.lastOrNull()?.second
        val list = if (parent == null) root else {
            parent.lastChild as? BulletList ?: BulletList().apply {
                isTight = true
                marker = "-"
                parent.appendChild(this)
            }
        }
        val item = ListItem()
        val paragraph = Paragraph()
        val text = Text(entry.label)
        if (entry.destination == null) paragraph.appendChild(text) else {
            paragraph.appendChild(Link(entry.destination, null).apply { appendChild(text) })
        }
        item.appendChild(paragraph)
        list.appendChild(item)
        ancestors.add(entry.level to item)
    }
    return root
}

/** Encode a literal ID as a URI fragment, including percent signs and fragment delimiters. */
internal fun fragment(id: String): String = buildString {
    append('#')
    for (byte in id.encodeToByteArray()) {
        val value = byte.toInt() and 0xff
        val char = value.toChar()
        if (char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' || char in "-._~") append(char) else {
            append('%')
            append("0123456789ABCDEF"[value shr 4])
            append("0123456789ABCDEF"[value and 15])
        }
    }
}
