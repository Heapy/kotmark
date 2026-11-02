package io.heapy.kotmark.test

import io.heapy.kotmark.text.Characters
import kotlin.test.Test
import kotlin.test.assertTrue

class SanityTest {
    @Test
    fun smoke() {
        assertTrue(Characters.isSpaceOrTab(" ", 0))
    }
}
