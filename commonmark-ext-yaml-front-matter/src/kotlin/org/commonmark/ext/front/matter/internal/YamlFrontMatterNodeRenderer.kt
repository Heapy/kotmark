package org.commonmark.ext.front.matter.internal

import org.commonmark.ext.front.matter.YamlFrontMatterBlock
import org.commonmark.node.Node
import org.commonmark.renderer.NodeRenderer
import kotlin.reflect.KClass

abstract class YamlFrontMatterNodeRenderer : NodeRenderer {
    override val nodeTypes: Set<KClass<out Node>> = setOf(YamlFrontMatterBlock::class)
}
