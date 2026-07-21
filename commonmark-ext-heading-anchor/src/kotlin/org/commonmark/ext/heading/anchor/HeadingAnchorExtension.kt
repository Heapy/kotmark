package org.commonmark.ext.heading.anchor

import org.commonmark.Extension
import org.commonmark.ext.heading.anchor.internal.HeadingIdAttributeProvider
import org.commonmark.renderer.html.HtmlRenderer
import kotlin.jvm.JvmStatic

class HeadingAnchorExtension private constructor(builder: Builder) :
    HtmlRenderer.HtmlRendererExtension {
    private val defaultId = builder.defaultId
    private val idPrefix = builder.idPrefix
    private val idSuffix = builder.idSuffix

    override fun extend(rendererBuilder: HtmlRenderer.Builder) {
        rendererBuilder.attributeProviderFactory {
            HeadingIdAttributeProvider.create(defaultId, idPrefix, idSuffix)
        }
    }

    class Builder internal constructor() {
        internal var defaultId = "id"
        internal var idPrefix = ""
        internal var idSuffix = ""

        fun defaultId(value: String): Builder = apply {
            defaultId = value
        }

        fun idPrefix(value: String): Builder = apply {
            idPrefix = value
        }

        fun idSuffix(value: String): Builder = apply {
            idSuffix = value
        }

        fun build(): Extension = HeadingAnchorExtension(this)
    }

    companion object {
        @JvmStatic
        fun create(): Extension = HeadingAnchorExtension(Builder())

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
