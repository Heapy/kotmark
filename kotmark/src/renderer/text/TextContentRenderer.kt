package io.heapy.kotmark.renderer.text

import io.heapy.kotmark.Extension
import io.heapy.kotmark.internal.renderer.NodeRendererMap
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.Renderer
import kotlin.jvm.JvmStatic

class TextContentRenderer private constructor(builder: Builder) : Renderer {
    private val lineBreakRendering = builder.lineBreakRendering
    private val nodeRendererFactories =
        buildList {
            addAll(builder.nodeRendererFactories)
            add(TextContentNodeRendererFactory(::CoreTextContentNodeRenderer))
        }

    override fun render(node: Node, output: Appendable) {
        RendererContext(TextContentWriter(output, lineBreakRendering)).render(node)
    }

    override fun render(node: Node): String =
        buildString {
            render(node, this)
        }

    class Builder internal constructor() {
        internal val nodeRendererFactories = mutableListOf<TextContentNodeRendererFactory>()
        internal var lineBreakRendering = LineBreakRendering.COMPACT

        fun build(): TextContentRenderer = TextContentRenderer(this)

        fun lineBreakRendering(lineBreakRendering: LineBreakRendering): Builder = apply {
            this.lineBreakRendering = lineBreakRendering
        }

        @Deprecated("Use lineBreakRendering(LineBreakRendering.STRIP) instead")
        fun stripNewlines(stripNewlines: Boolean): Builder = apply {
            lineBreakRendering =
                if (stripNewlines) {
                    LineBreakRendering.STRIP
                } else {
                    LineBreakRendering.COMPACT
                }
        }

        fun nodeRendererFactory(
            nodeRendererFactory: TextContentNodeRendererFactory,
        ): Builder = apply {
            nodeRendererFactories.add(nodeRendererFactory)
        }

        fun extensions(extensions: Iterable<Extension>): Builder = apply {
            for (extension in extensions) {
                if (extension is TextContentRendererExtension) {
                    extension.extend(this)
                }
            }
        }
    }

    fun interface TextContentRendererExtension : Extension {
        fun extend(rendererBuilder: Builder)
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

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
