package io.heapy.kotmark.ext.autolink

import io.heapy.kotmark.Extension
import io.heapy.kotmark.KotmarkDsl
import io.heapy.kotmark.ext.autolink.internal.AutolinkPostProcessor
import io.heapy.kotmark.parser.Parser
import kotlin.jvm.JvmStatic

class AutolinkExtension private constructor(configuration: Configuration) : Parser.ParserExtension {
    private val linkTypes = configuration.linkTypes.toSet()

    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.postProcessor(AutolinkPostProcessor(linkTypes))
    }

    @KotmarkDsl
    class Configuration internal constructor() {
        var linkTypes: Set<AutolinkType> = AutolinkType.entries.toSet()
            set(value) {
                require(value.isNotEmpty()) { "linkTypes must not be empty" }
                field = value.toSet()
            }

        internal fun create(): AutolinkExtension = AutolinkExtension(this)
    }

    companion object {
        @JvmStatic
        fun create(): Extension = autolinkExtension()
    }
}

/** Creates [AutolinkExtension] with the supplied configuration. */
fun autolinkExtension(configure: AutolinkExtension.Configuration.() -> Unit = {}): AutolinkExtension =
    AutolinkExtension.Configuration().apply(configure).create()
