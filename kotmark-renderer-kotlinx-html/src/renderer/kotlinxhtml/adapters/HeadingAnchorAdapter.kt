package io.heapy.kotmark.renderer.kotlinxhtml.adapters

import io.heapy.kotmark.ext.heading.anchor.idGenerator
import io.heapy.kotmark.node.AbstractVisitor
import io.heapy.kotmark.node.Code
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.Text
import io.heapy.kotmark.renderer.html.AttributeProvider
import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlAdapter

/** Generates heading IDs with a fresh duplicate counter per document render. */
fun headingAnchorAdapter(defaultId: String = "id", idPrefix: String = "", idSuffix: String = ""): KotlinxHtmlAdapter =
    KotlinxHtmlAdapter { configuration ->
        configuration.attributeProviderFactory {
            val generator = idGenerator { this.defaultId = defaultId; prefix = idPrefix; suffix = idSuffix }
            AttributeProvider { node, tag, attributes ->
                if (node is Heading && tag == "h${node.level}") {
                    val title = buildString {
                        node.accept(object : AbstractVisitor() {
                            override fun visit(text: Text) { append(text.literal.orEmpty()) }
                            override fun visit(code: Code) { append(code.literal.orEmpty()) }
                        })
                    }
                    attributes["id"] = generator.generateId(title.trim().lowercase())
                }
            }
        }
    }
