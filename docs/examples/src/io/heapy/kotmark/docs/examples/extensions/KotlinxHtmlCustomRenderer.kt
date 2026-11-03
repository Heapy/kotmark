package io.heapy.kotmark.docs.examples.extensions

// region kotlinx-html-custom
import io.heapy.kotmark.node.Code
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlRenderer
import io.heapy.kotmark.renderer.kotlinxhtml.kotlinxHtmlRenderer
import io.heapy.kotmark.renderer.kotlinxhtml.nodeRenderer

fun customCodeRenderer(): KotlinxHtmlRenderer = kotlinxHtmlRenderer {
    nodeRenderer<Code> { code ->
        element(code, "code", mapOf("class" to "inline-code")) {
            text(code.literal.orEmpty())
        }
    }
}
// endregion kotlinx-html-custom
