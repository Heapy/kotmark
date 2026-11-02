package io.heapy.kotmark.ext.image.attributes

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ext.image.attributes.internal.ImageAttributesAttributeProvider
import io.heapy.kotmark.ext.image.attributes.internal.ImageAttributesDelimiterProcessor
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.html.HtmlRenderer
import kotlin.jvm.JvmStatic

class ImageAttributesExtension private constructor() :
    Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension {
    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.customDelimiterProcessor(ImageAttributesDelimiterProcessor())
    }

    override fun extend(rendererConfiguration: HtmlRenderer.Configuration) {
        rendererConfiguration.attributeProviderFactory { ImageAttributesAttributeProvider.create() }
    }

    companion object {
        @JvmStatic
        fun create(): Extension = ImageAttributesExtension()
    }
}
