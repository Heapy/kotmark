package org.commonmark.internal.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UnicodeEscapingTest {
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
