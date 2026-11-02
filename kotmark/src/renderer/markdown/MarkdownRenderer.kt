package io.heapy.kotmark.renderer.markdown

import io.heapy.kotmark.Extension
import io.heapy.kotmark.internal.renderer.NodeRendererMap
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.Renderer
import kotlin.jvm.JvmStatic

class MarkdownRenderer private constructor(builder: Builder) : Renderer {
    private val nodeRendererFactories =
        buildList {
            addAll(builder.nodeRendererFactories)
            add(
                object : MarkdownNodeRendererFactory {
                    override fun create(context: MarkdownNodeRendererContext): NodeRenderer =
                        CoreMarkdownNodeRenderer(context)

                    override val specialCharacters: Set<Char> = emptySet()
                },
            )
        }

    override fun render(node: Node, output: Appendable) {
        RendererContext(MarkdownWriter(output)).render(node)
    }

    override fun render(node: Node): String =
        buildString {
            render(node, this)
        }

    class Builder internal constructor() {
        internal val nodeRendererFactories = mutableListOf<MarkdownNodeRendererFactory>()

        fun build(): MarkdownRenderer = MarkdownRenderer(this)

        fun nodeRendererFactory(
            nodeRendererFactory: MarkdownNodeRendererFactory,
        ): Builder = apply {
            nodeRendererFactories.add(nodeRendererFactory)
        }

        fun extensions(extensions: Iterable<Extension>): Builder = apply {
            for (extension in extensions) {
                if (extension is MarkdownRendererExtension) {
                    extension.extend(this)
                }
            }
        }
    }

    fun interface MarkdownRendererExtension : Extension {
        fun extend(rendererBuilder: Builder)
    }

    private inner class RendererContext(
        override val writer: MarkdownWriter,
    ) : MarkdownNodeRendererContext {
        override val specialCharacters: Set<Char> =
            buildSet {
                for (factory in nodeRendererFactories) {
                    addAll(factory.specialCharacters)
                }
            }

        private val nodeRendererMap =
            NodeRendererMap().also { map ->
                for (factory in nodeRendererFactories) {
                    map.add(factory.create(this))
                }
            }

        override fun render(node: Node) {
            nodeRendererMap.render(node)
        }
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
