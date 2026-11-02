package io.heapy.kotmark.text

/**
 * Char matcher that can match ASCII characters efficiently.
 */
class AsciiMatcher private constructor(private val set: BooleanArray) : CharMatcher {

    override fun matches(c: Char): Boolean {
        val code = c.code
        return code in 0..127 && set[code]
    }

    fun newBuilder(): Builder = Builder(set.copyOf())

    class Builder internal constructor(set: BooleanArray) {
        private val set: BooleanArray = set.copyOf()

        fun c(c: Char): Builder {
            require(c.code <= 127) { "Can only match ASCII characters" }
            set[c.code] = true
            return this
        }

        fun anyOf(s: String): Builder {
            for (i in s.indices) {
                c(s[i])
            }
            return this
        }

        fun anyOf(characters: Collection<Char>): Builder {
            for (c in characters) {
                c(c)
            }
            return this
        }

        fun range(from: Char, toInclusive: Char): Builder {
            var c = from
            while (c <= toInclusive) {
                c(c)
                c = (c.code + 1).toChar()
            }
            return this
        }

        fun build(): AsciiMatcher = AsciiMatcher(set)
    }

    companion object {
        fun builder(): Builder = Builder(BooleanArray(128))
        fun builder(matcher: AsciiMatcher): Builder = Builder(matcher.set)
    }
}
