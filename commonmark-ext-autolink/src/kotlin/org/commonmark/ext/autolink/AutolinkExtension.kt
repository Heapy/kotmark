package org.commonmark.ext.autolink

import org.commonmark.Extension
import org.commonmark.ext.autolink.internal.AutolinkPostProcessor
import org.commonmark.parser.Parser
import kotlin.jvm.JvmStatic

class AutolinkExtension private constructor(builder: Builder) : Parser.ParserExtension {
    private val linkTypes = builder.linkTypes.toSet()

    override fun extend(parserBuilder: Parser.Builder) {
        parserBuilder.postProcessor(AutolinkPostProcessor(linkTypes))
    }

    class Builder internal constructor() {
        internal var linkTypes: Set<AutolinkType> = AutolinkType.entries.toSet()

        fun linkTypes(vararg linkTypes: AutolinkType): Builder =
            linkTypes(linkTypes.toSet())

        fun linkTypes(linkTypes: Set<AutolinkType>): Builder = apply {
            require(linkTypes.isNotEmpty()) { "linkTypes must not be empty" }
            this.linkTypes = linkTypes.toSet()
        }

        fun build(): Extension = AutolinkExtension(this)
    }

    companion object {
        @JvmStatic
        fun create(): Extension = builder().build()

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
