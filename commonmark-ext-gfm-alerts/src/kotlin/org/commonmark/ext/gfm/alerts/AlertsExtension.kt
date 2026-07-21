package org.commonmark.ext.gfm.alerts

import org.commonmark.Extension
import org.commonmark.ext.gfm.alerts.internal.AlertBlockParser
import org.commonmark.ext.gfm.alerts.internal.AlertHtmlNodeRenderer
import org.commonmark.ext.gfm.alerts.internal.AlertMarkdownNodeRenderer
import org.commonmark.ext.gfm.alerts.internal.AlertTextContentNodeRenderer
import org.commonmark.parser.Parser
import org.commonmark.renderer.NodeRenderer
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.renderer.markdown.MarkdownNodeRendererContext
import org.commonmark.renderer.markdown.MarkdownNodeRendererFactory
import org.commonmark.renderer.markdown.MarkdownRenderer
import org.commonmark.renderer.text.TextContentRenderer
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

class AlertsExtension private constructor(builder: Builder) :
    Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension,
    TextContentRenderer.TextContentRendererExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    private val allowedTypes = builder.allowedTypes.toMap()
    private val customTitlesAllowed = builder.customTitlesAllowed
    private val nestedAlertsAllowed = builder.nestedAlertsAllowed

    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.customBlockParserFactory(
            AlertBlockParser.Factory(
                allowedTypes.keys,
                customTitlesAllowed,
                nestedAlertsAllowed,
            ),
        )
    }

    override fun extend(rendererBuilder: HtmlRenderer.Builder) {
        rendererBuilder.nodeRendererFactory { context ->
            AlertHtmlNodeRenderer(context, allowedTypes)
        }
    }

    override fun extend(rendererBuilder: TextContentRenderer.Builder) {
        rendererBuilder.nodeRendererFactory { context ->
            AlertTextContentNodeRenderer(context, allowedTypes)
        }
    }

    override fun extend(rendererBuilder: MarkdownRenderer.Builder) {
        rendererBuilder.nodeRendererFactory(
            object : MarkdownNodeRendererFactory {
                override val specialCharacters: Set<Char> = emptySet()

                override fun create(context: MarkdownNodeRendererContext): NodeRenderer =
                    AlertMarkdownNodeRenderer(context)
            },
        )
    }

    class Builder internal constructor() {
        internal var allowedTypes = STANDARD_TYPES.toMutableMap()
        internal var customTitlesAllowed = false
        internal var nestedAlertsAllowed = false

        fun setAllowedTypes(allowedTypes: Map<String, String>): Builder = apply {
            require(allowedTypes.isNotEmpty()) { "allowedTypes must not be empty" }
            for ((type, title) in allowedTypes) {
                validateTypeAndTitle(type, title)
            }
            this.allowedTypes = allowedTypes.toMutableMap()
        }

        fun addCustomType(type: String, title: String): Builder = apply {
            validateTypeAndTitle(type, title)
            allowedTypes[type] = title
        }

        fun allowCustomTitles(allow: Boolean): Builder = apply {
            customTitlesAllowed = allow
        }

        fun allowNestedAlerts(allow: Boolean): Builder = apply {
            nestedAlertsAllowed = allow
        }

        fun build(): Extension = AlertsExtension(this)

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
        fun create(): Extension = builder().build()

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
