package io.heapy.kotmark.ext.heading.anchor

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ExtensionsBuilder
import io.heapy.kotmark.KotmarkDsl
import io.heapy.kotmark.ext.heading.anchor.internal.HeadingIdAttributeProvider
import io.heapy.kotmark.renderer.html.HtmlRenderer
import kotlin.jvm.JvmStatic

class HeadingAnchorExtension private constructor(configuration: Configuration) :
    HtmlRenderer.HtmlRendererExtension {
    private val defaultId = configuration.defaultId
    private val idPrefix = configuration.idPrefix
    private val idSuffix = configuration.idSuffix

    override fun extend(rendererConfiguration: HtmlRenderer.Configuration) {
        rendererConfiguration.attributeProviderFactory {
            HeadingIdAttributeProvider.create(defaultId, idPrefix, idSuffix)
        }
    }

    @KotmarkDsl
    class Configuration internal constructor() {
        var defaultId = "id"
        var idPrefix = ""
        var idSuffix = ""

        internal fun create(): HeadingAnchorExtension = HeadingAnchorExtension(this)
    }

    companion object {
        @JvmStatic
        fun create(): Extension = headingAnchorExtension()
    }
}

/** Creates [HeadingAnchorExtension] with the supplied configuration. */
fun headingAnchorExtension(configure: HeadingAnchorExtension.Configuration.() -> Unit = {}): HeadingAnchorExtension =
    HeadingAnchorExtension.Configuration().apply(configure).create()

/** Creates and appends [HeadingAnchorExtension] with the supplied configuration. */
fun ExtensionsBuilder.headingAnchor(configure: HeadingAnchorExtension.Configuration.() -> Unit = {}) {
    add(headingAnchorExtension(configure))
}
