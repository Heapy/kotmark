package org.commonmark.internal.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UnicodeEscapingTest {
    @Test
    fun labelTrimmingOnlyRemovesAsciiControlsAndSpace() {
        assertEquals("\u00A0LABEL\u00A0", Escaping.normalizeLabelContent("\u00A0label\u00A0"))
        assertEquals("\u2003LABEL\u2003", Escaping.normalizeLabelContent("\u2003label\u2003"))
        assertEquals("LABEL", Escaping.normalizeLabelContent("\u0000\u001F label \u0000\u001F"))
        assertEquals("A B", Escaping.normalizeLabelContent(" a\t\r\n b "))
    }

    @Test
    fun invalidNumericEntitiesBecomeReplacementCharacters() {
        for (entity in listOf("&#0;", "&#1114112;", "&#9999999;", "&#x110000;", "&#xFFFFFF;")) {
            assertEquals("\uFFFD", Html5Entities.entityToString(entity), entity)
        }
        assertEquals("\uDBFF\uDFFF", Html5Entities.entityToString("&#x10FFFF;"))
        assertEquals("\uD835\uDD04", Html5Entities.entityToString("&#x1D504;"))
        assertFailsWith<IllegalArgumentException> { codePointToUtf(-1) }
        assertFailsWith<IllegalArgumentException> { codePointToUtf(0x110000) }
    }
}
