package org.commonmark.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnicodeCodePointTest {
    @Test
    fun supplementaryCodePointsDoNotAliasAscii() {
        for (character in "$+<=>^`|~ \t\n\r\u000C") {
            val codePoint = 0x20000 + character.code
            assertFalse(Characters.isPunctuationCodePoint(codePoint), "U+${codePoint.toString(16)}")
            assertFalse(Characters.isWhitespaceCodePoint(codePoint), "U+${codePoint.toString(16)}")
        }
    }

    @Test
    fun supplementaryCategoriesUseTheWholeCodePoint() {
        assertEquals(CharacterCategory.OTHER_PUNCTUATION, getType(0x10100))
        assertEquals(CharacterCategory.OTHER_SYMBOL, getType(0x1F600))
        assertEquals(CharacterCategory.MATH_SYMBOL, getType(0x1D6C1))
        assertTrue(Characters.isPunctuationCodePoint(0x10100))
        assertTrue(Characters.isPunctuationCodePoint(0x1F600))
        assertTrue(isLetter(0x20000))
        assertFalse(isLetter(0x1F600))
        assertFalse(Characters.isWhitespaceCodePoint(0x20000))
    }
}
