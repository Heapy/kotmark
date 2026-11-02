package io.heapy.kotmark.internal.util

internal actual fun codePointToUtf(codePoint: Int): String = String(Character.toChars(codePoint))
