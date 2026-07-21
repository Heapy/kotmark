package org.commonmark.internal.util

internal actual fun codePointToUtf(codePoint: Int): String =
    js("String.fromCodePoint(codePoint)").unsafeCast<String>()
