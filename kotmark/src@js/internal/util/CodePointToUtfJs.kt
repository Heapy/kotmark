package io.heapy.kotmark.internal.util

internal actual fun codePointToUtf(codePoint: Int): String {
    require(codePoint in 0..0x10FFFF) { "Invalid Unicode code point: $codePoint" }
    return js("String.fromCodePoint(codePoint)").unsafeCast<String>()
}
