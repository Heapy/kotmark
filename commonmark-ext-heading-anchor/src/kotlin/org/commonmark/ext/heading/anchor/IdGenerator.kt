package org.commonmark.ext.heading.anchor

import org.commonmark.internal.util.UnicodeCategories
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
            val normalized = text.lowercase().replace(' ', '-')
            var index = 0
            while (index < normalized.length) {
                val first = normalized[index]
                val paired = first.isHighSurrogate() && index + 1 < normalized.length &&
                    normalized[index + 1].isLowSurrogate()
                val length = if (paired) 2 else 1
                val codePoint = if (paired) {
                    0x10000 + ((first.code - 0xD800) shl 10) + normalized[index + 1].code - 0xDC00
                } else {
                    first.code
                }
                val category = UnicodeCategories.category(codePoint)
                if (first == '-' || category in ALLOWED_CATEGORIES) {
                    append(normalized, index, index + length)
                }
                index += length
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
        private val ALLOWED_CATEGORIES = setOf(
            CharCategory.UPPERCASE_LETTER, CharCategory.LOWERCASE_LETTER,
            CharCategory.TITLECASE_LETTER, CharCategory.MODIFIER_LETTER,
            CharCategory.OTHER_LETTER, CharCategory.DECIMAL_DIGIT_NUMBER,
            CharCategory.NON_SPACING_MARK, CharCategory.COMBINING_SPACING_MARK,
            CharCategory.ENCLOSING_MARK, CharCategory.CONNECTOR_PUNCTUATION,
        )

        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
