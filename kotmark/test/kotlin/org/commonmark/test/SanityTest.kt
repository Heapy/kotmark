package org.commonmark.test

import org.commonmark.text.Characters
import kotlin.test.Test
import kotlin.test.assertTrue

class SanityTest {
    @Test
    fun smoke() {
        assertTrue(Characters.isSpaceOrTab(" ", 0))
    }
}
