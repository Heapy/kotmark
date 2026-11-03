package io.heapy.kotmark.ext.gfm.alerts

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ExtensionsBuilder
import io.heapy.kotmark.KotmarkDsl
import io.heapy.kotmark.ext.gfm.alerts.internal.AlertBlockParser
import io.heapy.kotmark.ext.gfm.alerts.internal.AlertHtmlNodeRenderer
import io.heapy.kotmark.ext.gfm.alerts.internal.AlertMarkdownNodeRenderer
import io.heapy.kotmark.ext.gfm.alerts.internal.AlertTextContentNodeRenderer
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererContext
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererFactory
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import io.heapy.kotmark.renderer.text.TextContentRenderer
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

class AlertsExtension private constructor(configuration: Configuration) :
    Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension,
    TextContentRenderer.TextContentRendererExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    private val allowedTypes = configuration.allowedTypes.toMap()
    private val customTitlesAllowed = configuration.customTitlesAllowed
    private val nestedAlertsAllowed = configuration.nestedAlertsAllowed

    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.customBlockParserFactory(
            AlertBlockParser.Factory(
                allowedTypes.keys,
                customTitlesAllowed,
                nestedAlertsAllowed,
            ),
        )
    }

    override fun extend(rendererConfiguration: HtmlRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory { context ->
            AlertHtmlNodeRenderer(context, allowedTypes)
        }
    }

    override fun extend(rendererConfiguration: TextContentRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory { context ->
            AlertTextContentNodeRenderer(context, allowedTypes)
        }
    }

    override fun extend(rendererConfiguration: MarkdownRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(
            object : MarkdownNodeRendererFactory {
                override val specialCharacters: Set<Char> = emptySet()

                override fun create(context: MarkdownNodeRendererContext): NodeRenderer =
                    AlertMarkdownNodeRenderer(context)
            },
        )
    }

    @KotmarkDsl
    class Configuration internal constructor() {
        var allowedTypes: Map<String, String> = STANDARD_TYPES.toMap()
            set(value) {
                require(value.isNotEmpty()) { "allowedTypes must not be empty" }
                for ([type, title] in value) {
                    validateTypeAndTitle(type, title)
                }
                field = value.toMap()
            }
        var customTitlesAllowed = false
        var nestedAlertsAllowed = false

        fun addCustomType(type: String, title: String) {
            validateTypeAndTitle(type, title)
            allowedTypes = allowedTypes + (type to title)
        }

        internal fun create(): AlertsExtension = AlertsExtension(this)

        private fun validateTypeAndTitle(type: String, title: String) {
            require(type.isNotEmpty()) { "Type must not be empty" }
            require(type == type.uppercase()) { "Type must be uppercase: $type" }
            require(title.isNotEmpty()) { "Default title must not be empty: $type" }
        }
    }

    companion object {
        @JvmField
        val STANDARD_TYPES: Map<String, String> =
            linkedMapOf(
                "NOTE" to "Note",
                "TIP" to "Tip",
                "IMPORTANT" to "Important",
                "WARNING" to "Warning",
                "CAUTION" to "Caution",
            )

        @JvmStatic
        fun create(): Extension = alertsExtension()
    }
}

/** Creates [AlertsExtension] with the supplied configuration. */
fun alertsExtension(configure: AlertsExtension.Configuration.() -> Unit = {}): AlertsExtension =
    AlertsExtension.Configuration().apply(configure).create()

/** Creates and appends [AlertsExtension] with the supplied configuration. */
fun ExtensionsBuilder.alerts(configure: AlertsExtension.Configuration.() -> Unit = {}) {
    add(alertsExtension(configure))
}
