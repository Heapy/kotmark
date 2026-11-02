package io.heapy.kotmark.renderer.markdown

import io.heapy.kotmark.Extension
import io.heapy.kotmark.KotmarkDsl
import io.heapy.kotmark.internal.renderer.NodeRendererMap
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.Renderer

class MarkdownRenderer private constructor(configuration: Configuration) : Renderer {
    private val nodeRendererFactories =
        buildList {
            addAll(configuration.nodeRendererFactories)
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

    @KotmarkDsl
    class Configuration internal constructor() {
        internal val nodeRendererFactories = mutableListOf<MarkdownNodeRendererFactory>()

        internal fun create(): MarkdownRenderer = MarkdownRenderer(this)

        fun nodeRendererFactory(
            nodeRendererFactory: MarkdownNodeRendererFactory,
        ) {
            nodeRendererFactories.add(nodeRendererFactory)
        }

        fun extensions(extensions: Iterable<Extension>) {
            for (extension in extensions) {
                if (extension is MarkdownRendererExtension) {
                    extension.extend(this)
                }
            }
        }
    }

    fun interface MarkdownRendererExtension : Extension {
        fun extend(rendererConfiguration: Configuration)
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
}

/** Creates [MarkdownRenderer] with the supplied configuration. */
fun markdownRenderer(configure: MarkdownRenderer.Configuration.() -> Unit = {}): MarkdownRenderer =
    MarkdownRenderer.Configuration().apply(configure).create()
