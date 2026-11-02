package io.heapy.kotmark.ext.math.internal

import io.heapy.kotmark.ext.math.InlineMath
import io.heapy.kotmark.ext.math.MathBlock
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import kotlin.reflect.KClass

internal abstract class MathNodeRenderer : NodeRenderer {
    final override val nodeTypes: Set<KClass<out Node>> = setOf(InlineMath::class, MathBlock::class)
}
