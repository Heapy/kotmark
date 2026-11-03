package io.heapy.kotmark.renderer.kotlinxhtml.adapters

import io.heapy.kotmark.ext.front.matter.YamlFrontMatterBlock
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterNode
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlAdapter
import io.heapy.kotmark.renderer.kotlinxhtml.nodeRenderer

/** Front matter is document metadata and has no HTML representation. */
fun yamlFrontMatterAdapter(): KotlinxHtmlAdapter = KotlinxHtmlAdapter { configuration ->
    configuration.nodeRenderer<YamlFrontMatterBlock> { }
    configuration.nodeRenderer<YamlFrontMatterNode> { }
}
