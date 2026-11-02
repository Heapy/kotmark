package io.heapy.kotmark.ext.ins.internal

import io.heapy.kotmark.ext.ins.Ins
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import kotlin.reflect.KClass

abstract class InsNodeRenderer : NodeRenderer {
    final override val nodeTypes: Set<KClass<out Node>> = setOf(Ins::class)
}
