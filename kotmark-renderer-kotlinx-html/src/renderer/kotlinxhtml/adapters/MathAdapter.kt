package io.heapy.kotmark.renderer.kotlinxhtml.adapters

import io.heapy.kotmark.ext.math.InlineMath
import io.heapy.kotmark.ext.math.MathBlock
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlAdapter
import io.heapy.kotmark.renderer.kotlinxhtml.nodeRenderer

/** Emits escaped TeX source in math containers. Typesetting/KaTeX assets belong to the application. */
fun mathAdapter(): KotlinxHtmlAdapter = KotlinxHtmlAdapter { configuration ->
    configuration.nodeRenderer<InlineMath> { node ->
        element(node, "span", mapOf("class" to "math math-inline")) { text(node.literal) }
    }
    configuration.nodeRenderer<MathBlock> { node ->
        element(node, "div", mapOf("class" to "math math-block")) { text(node.literal) }
    }
}
