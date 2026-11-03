package io.heapy.kotmark.renderer.kotlinxhtml.adapters

import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlAdapter
import io.heapy.kotmark.renderer.kotlinxhtml.internal.FootnoteNodeRenderer

/** Numbers references in encounter order and appends only referenced definitions. */
fun footnotesAdapter(): KotlinxHtmlAdapter = KotlinxHtmlAdapter { configuration ->
    configuration.nodeRendererFactory(::FootnoteNodeRenderer)
}
