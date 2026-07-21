package org.commonmark.ext.heading.anchor

import kotlin.jvm.JvmStatic

class IdGenerator private constructor(builder: Builder) {
    private val identities = mutableMapOf<String, Int>()
    private val defaultIdentifier = builder.defaultIdentifier
    private val prefix = builder.prefix
    private val suffix = builder.suffix

    fun generateId(text: String?): String {
        var normalized = text?.let(::normalizeText) ?: defaultIdentifier
        if (normalized.isEmpty()) {
            normalized = defaultIdentifier
        }
        val count = identities[normalized]
        return if (count == null) {
            identities[normalized] = 1
            "$prefix$normalized$suffix"
        } else {
            identities[normalized] = count + 1
            "$prefix$normalized-$count$suffix"
        }
    }

    private fun normalizeText(text: String): String =
        buildString {
            for (character in text.lowercase().replace(' ', '-')) {
                if (
                    character == '-' ||
                    character == '_' ||
                    character.isLetterOrDigit() ||
                    character.category in ALLOWED_CATEGORIES
                ) {
                    append(character)
                }
            }
        }

    class Builder internal constructor() {
        internal var defaultIdentifier = "id"
        internal var prefix = ""
        internal var suffix = ""

        fun defaultId(defaultId: String): Builder = apply {
            defaultIdentifier = defaultId
        }

        fun prefix(prefix: String): Builder = apply {
            this.prefix = prefix
        }

        fun suffix(suffix: String): Builder = apply {
            this.suffix = suffix
        }

        fun build(): IdGenerator = IdGenerator(this)
    }

    companion object {
        private val ALLOWED_CATEGORIES =
            setOf(
                CharCategory.NON_SPACING_MARK,
                CharCategory.COMBINING_SPACING_MARK,
                CharCategory.ENCLOSING_MARK,
                CharCategory.CONNECTOR_PUNCTUATION,
            )

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
