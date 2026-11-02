package io.heapy.kotmark.internal.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnicodeCategoriesTest {
    @Test
    fun unicode18SupplementaryCharactersAreRecognized() {
        // These assertions target the bundled data, independently of the host JDK/JS Unicode version.
        assertEquals(CharCategory.OTHER_SYMBOL, UnicodeCategories.category(0x1FADD)) // PICKLE
        assertEquals(CharCategory.OTHER_SYMBOL, UnicodeCategories.category(0x1FAF9)) // LEFTWARDS THUMB SIGN
        assertTrue(UnicodeCategories.isLetter(0x18E00)) // JURCHEN
    }

    @Test
    fun unicode18SealRangeIncludesBothEndsAndExcludesNeighbours() {
        assertEquals(CharCategory.UNASSIGNED, UnicodeCategories.category(0x3CFFF))
        assertEquals(CharCategory.OTHER_LETTER, UnicodeCategories.category(0x3D000))
        assertEquals(CharCategory.OTHER_LETTER, UnicodeCategories.category(0x3E000))
        assertEquals(CharCategory.OTHER_LETTER, UnicodeCategories.category(0x3FC3F))
        assertEquals(CharCategory.UNASSIGNED, UnicodeCategories.category(0x3FC40))
    }
}
