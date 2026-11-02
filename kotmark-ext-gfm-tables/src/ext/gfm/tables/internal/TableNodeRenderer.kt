package io.heapy.kotmark.ext.gfm.tables.internal

import io.heapy.kotmark.ext.gfm.tables.TableBlock
import io.heapy.kotmark.ext.gfm.tables.TableBody
import io.heapy.kotmark.ext.gfm.tables.TableCell
import io.heapy.kotmark.ext.gfm.tables.TableHead
import io.heapy.kotmark.ext.gfm.tables.TableRow
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import kotlin.reflect.KClass

abstract class TableNodeRenderer : NodeRenderer {
    override val nodeTypes: Set<KClass<out Node>> =
        setOf(
            TableBlock::class,
            TableHead::class,
            TableBody::class,
            TableRow::class,
            TableCell::class,
        )

    final override fun render(node: Node) {
        when (node) {
            is TableBlock -> renderBlock(node)
            is TableHead -> renderHead(node)
            is TableBody -> renderBody(node)
            is TableRow -> renderRow(node)
            is TableCell -> renderCell(node)
        }
    }

    protected abstract fun renderBlock(node: TableBlock)

    protected abstract fun renderHead(node: TableHead)

    protected abstract fun renderBody(node: TableBody)

    protected abstract fun renderRow(node: TableRow)

    protected abstract fun renderCell(node: TableCell)
}
