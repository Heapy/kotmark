package io.heapy.kotmark.ext.gfm.tables

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ExtensionsBuilder
import io.heapy.kotmark.KotmarkDsl
import io.heapy.kotmark.ext.gfm.tables.internal.TableBlockParser
import io.heapy.kotmark.ext.gfm.tables.internal.TableHtmlNodeRenderer
import io.heapy.kotmark.ext.gfm.tables.internal.TableMarkdownNodeRenderer
import io.heapy.kotmark.ext.gfm.tables.internal.TableTextContentNodeRenderer
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererContext
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererFactory
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.renderer.text.TextContentRenderer
import kotlin.jvm.JvmStatic

class TablesExtension private constructor(configuration: Configuration) :
    Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension,
    TextContentRenderer.TextContentRendererExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    private val maxCells = configuration.maxCells

    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.customBlockParserFactory(TableBlockParser.Factory(maxCells))
    }

    override fun extend(rendererConfiguration: HtmlRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(::TableHtmlNodeRenderer)
    }

    override fun extend(rendererConfiguration: TextContentRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(::TableTextContentNodeRenderer)
    }

    override fun extend(rendererConfiguration: MarkdownRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(
            object : MarkdownNodeRendererFactory {
                override val specialCharacters: Set<Char> = setOf('|')

                override fun create(context: MarkdownNodeRendererContext): NodeRenderer =
                    TableMarkdownNodeRenderer(context)
            },
        )
    }

    @KotmarkDsl
    class Configuration internal constructor() {
        /**
         * Maximum number of cells parsed per table, including headers and omitted body cells.
         * Parsing throws [IllegalArgumentException] if this limit is exceeded. Defaults to
         * [DEFAULT_MAX_CELLS]; use `null` for no limit.
         */
        var maxCells: Int? = DEFAULT_MAX_CELLS

        internal fun create(): TablesExtension = TablesExtension(this)
    }

    companion object {
        /** Default maximum number of parsed cells per table. */
        const val DEFAULT_MAX_CELLS: Int = 1_000_000

        @JvmStatic
        fun create(): Extension = tablesExtension()
    }
}

/** Creates [TablesExtension] with the supplied configuration. */
fun tablesExtension(configure: TablesExtension.Configuration.() -> Unit = {}): TablesExtension =
    TablesExtension.Configuration().apply(configure).create()

/** Creates and appends [TablesExtension] with the supplied configuration. */
fun ExtensionsBuilder.tables(configure: TablesExtension.Configuration.() -> Unit = {}) {
    add(tablesExtension(configure))
}
