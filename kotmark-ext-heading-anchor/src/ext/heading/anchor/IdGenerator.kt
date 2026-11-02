package io.heapy.kotmark.ext.heading.anchor

import io.heapy.kotmark.KotmarkDsl
import io.heapy.kotmark.internal.util.UnicodeCategories

class IdGenerator private constructor(configuration: Configuration) {
    private val identities = mutableMapOf<String, Int>()
    private val defaultIdentifier = configuration.defaultId
    private val prefix = configuration.prefix
    private val suffix = configuration.suffix

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

    @KotmarkDsl
    class Configuration internal constructor() {
        var defaultId = "id"
        var prefix = ""
        var suffix = ""

        internal fun create(): IdGenerator = IdGenerator(this)
    }

    companion object {
        private val ALLOWED_CATEGORIES = setOf(
            CharCategory.UPPERCASE_LETTER, CharCategory.LOWERCASE_LETTER,
            CharCategory.TITLECASE_LETTER, CharCategory.MODIFIER_LETTER,
            CharCategory.OTHER_LETTER, CharCategory.DECIMAL_DIGIT_NUMBER,
            CharCategory.NON_SPACING_MARK, CharCategory.COMBINING_SPACING_MARK,
            CharCategory.ENCLOSING_MARK, CharCategory.CONNECTOR_PUNCTUATION,
        )
    }
}

/** Creates [IdGenerator] with the supplied configuration. */
fun idGenerator(configure: IdGenerator.Configuration.() -> Unit = {}): IdGenerator =
    IdGenerator.Configuration().apply(configure).create()
