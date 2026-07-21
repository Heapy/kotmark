package org.commonmark.text

import java.lang.Character as JCharacter

internal actual fun codePointAt(s: CharSequence, index: Int): Int = JCharacter.codePointAt(s, index)

internal actual fun isLetter(codePoint: Int): Boolean = JCharacter.isLetter(codePoint)

internal actual fun getType(codePoint: Int): Int = JCharacter.getType(codePoint)

internal actual fun toChars(codePoint: Int): String = String(JCharacter.toChars(codePoint))

internal actual fun isSupplementaryCodePoint(codePoint: Int): Boolean = JCharacter.isSupplementaryCodePoint(codePoint)
