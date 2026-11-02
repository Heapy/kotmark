package io.heapy.kotmark.renderer.text

import io.heapy.kotmark.Extension
import io.heapy.kotmark.KotmarkDsl
import io.heapy.kotmark.internal.renderer.NodeRendererMap
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.Renderer

class TextContentRenderer private constructor(configuration: Configuration) : Renderer {
    private val lineBreakRendering = configuration.lineBreakRendering
    private val nodeRendererFactories =
        buildList {
            addAll(configuration.nodeRendererFactories)
            add(TextContentNodeRendererFactory(::CoreTextContentNodeRenderer))
        }

    override fun render(node: Node, output: Appendable) {
        RendererContext(TextContentWriter(output, lineBreakRendering)).render(node)
    }

    override fun render(node: Node): String =
        buildString {
            render(node, this)
        }

    @KotmarkDsl
    class Configuration internal constructor() {
        internal val nodeRendererFactories = mutableListOf<TextContentNodeRendererFactory>()
        var lineBreakRendering = LineBreakRendering.COMPACT

        internal fun create(): TextContentRenderer = TextContentRenderer(this)

        fun nodeRendererFactory(
            nodeRendererFactory: TextContentNodeRendererFactory,
        ) {
            nodeRendererFactories.add(nodeRendererFactory)
        }

        fun extensions(extensions: Iterable<Extension>) {
            for (extension in extensions) {
                if (extension is TextContentRendererExtension) {
                    extension.extend(this)
                }
            }
        }
    }

    fun interface TextContentRendererExtension : Extension {
        fun extend(rendererConfiguration: Configuration)
    }

    private inner class RendererContext(
        override val writer: TextContentWriter,
    ) : TextContentNodeRendererContext {
        override val lineBreakRendering: LineBreakRendering
            get() = this@TextContentRenderer.lineBreakRendering

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

/** Creates [TextContentRenderer] with the supplied configuration. */
fun textContentRenderer(configure: TextContentRenderer.Configuration.() -> Unit = {}): TextContentRenderer =
    TextContentRenderer.Configuration().apply(configure).create()
