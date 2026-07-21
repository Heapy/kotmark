package org.commonmark.ext.image.attributes

import org.commonmark.Extension
import org.commonmark.ext.image.attributes.internal.ImageAttributesAttributeProvider
import org.commonmark.ext.image.attributes.internal.ImageAttributesDelimiterProcessor
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import kotlin.jvm.JvmStatic

class ImageAttributesExtension private constructor() :
    Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension {
    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.customDelimiterProcessor(ImageAttributesDelimiterProcessor())
    }

    override fun extend(rendererBuilder: HtmlRenderer.Builder) {
        rendererBuilder.attributeProviderFactory { ImageAttributesAttributeProvider.create() }
    }

    companion object {
        @JvmStatic
        fun create(): Extension = ImageAttributesExtension()
    }
}
