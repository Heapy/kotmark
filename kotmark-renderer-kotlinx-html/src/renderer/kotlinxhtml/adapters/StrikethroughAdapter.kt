package io.heapy.kotmark.renderer.kotlinxhtml.adapters

import io.heapy.kotmark.ext.gfm.strikethrough.Strikethrough
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlAdapter
import io.heapy.kotmark.renderer.kotlinxhtml.nodeRenderer

fun strikethroughAdapter(): KotlinxHtmlAdapter = KotlinxHtmlAdapter { configuration ->
    configuration.nodeRenderer<Strikethrough> { node ->
        element(node, "del") { renderChildren(node) }
    }
}
