package io.heapy.kotmark.parser.beta

import io.heapy.kotmark.parser.SourceLine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PositionTest {
    @Test
    fun equalityAndHashCodeUseCoordinatesWithoutScannerIdentity() {
        val first = Scanner(listOf(SourceLine.of("abc", null)), 0, 1).position()
        val same = Scanner(listOf(SourceLine.of("xyz", null)), 0, 1).position()
        assertEquals(first, first)
        assertEquals(first, same)
        assertEquals(same, first)
        assertEquals(first.hashCode(), same.hashCode())
        assertEquals(1, setOf(first, same).size)
        assertEquals(0, first.compareTo(same))
        assertNotEquals(first, Position(0, 2))
        assertNotEquals(first, Position(1, 1))
        assertFalse(first.equals(null))
        assertFalse(first.equals("position"))
    }

    @Test
    fun positionsSortByLineThenUtf16Offset() {
        val scanner = Scanner(listOf(SourceLine.of("𝄞", null), SourceLine.of("x", null)), 0, 0)
        val positions = buildList {
            add(scanner.position())
            repeat(4) {
                scanner.next()
                add(scanner.position())
            }
        }
        assertEquals(
            listOf(Position(0, 0), Position(0, 1), Position(0, 2), Position(1, 0), Position(1, 1)),
            positions,
        )
        assertEquals(positions, positions.reversed().sorted())
        for ([earlier, later] in positions.zipWithNext()) {
            assertTrue(earlier < later)
            assertTrue(later > earlier)
        }
    }

    @Test
    fun comparisonDoesNotOverflow() {
        assertTrue(Position(Int.MIN_VALUE, Int.MAX_VALUE) < Position(Int.MAX_VALUE, Int.MIN_VALUE))
        assertTrue(Position(0, Int.MIN_VALUE) < Position(0, Int.MAX_VALUE))
        assertTrue(Position(0, Int.MAX_VALUE) > Position(0, Int.MIN_VALUE))
    }
}
