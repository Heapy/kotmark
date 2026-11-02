package io.heapy.kotmark.ext.front.matter.internal

import io.heapy.kotmark.ext.front.matter.YamlFrontMatterBlock
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import kotlin.reflect.KClass

abstract class YamlFrontMatterNodeRenderer : NodeRenderer {
    override val nodeTypes: Set<KClass<out Node>> = setOf(YamlFrontMatterBlock::class)
}
