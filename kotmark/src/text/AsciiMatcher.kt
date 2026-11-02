package io.heapy.kotmark.text

import io.heapy.kotmark.KotmarkDsl

/** Char matcher that can match ASCII characters efficiently. */
class AsciiMatcher private constructor(private val set: BooleanArray) : CharMatcher {
    override fun matches(c: Char): Boolean = c.code in 0..127 && set[c.code]

    @KotmarkDsl
    class Configuration internal constructor(matcher: AsciiMatcher?) {
        private val set = matcher?.set?.copyOf() ?: BooleanArray(128)

        fun c(c: Char) {
            require(c.code <= 127) { "Can only match ASCII characters" }
            set[c.code] = true
        }

        fun anyOf(s: String) {
            for (c in s) c(c)
        }

        fun anyOf(characters: Collection<Char>) {
            for (c in characters) c(c)
        }

        fun range(from: Char, toInclusive: Char) {
            for (c in from..toInclusive) c(c)
        }

        internal fun create(): AsciiMatcher = AsciiMatcher(set.copyOf())
    }
}

/** Creates an ASCII matcher, optionally extending a snapshot of [matcher]. */
fun asciiMatcher(
    matcher: AsciiMatcher? = null,
    configure: AsciiMatcher.Configuration.() -> Unit = {},
): AsciiMatcher = AsciiMatcher.Configuration(matcher).apply(configure).create()
