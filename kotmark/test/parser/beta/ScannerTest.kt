package io.heapy.kotmark.parser.beta

import io.heapy.kotmark.node.SourceSpan
import io.heapy.kotmark.parser.SourceLine
import io.heapy.kotmark.parser.SourceLines
import io.heapy.kotmark.text.CharMatcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ScannerTest {
    @Test
    fun prevReversesUtf16MovementAndLineBoundaries() {
        val inputs = listOf(
            emptyList(), listOf(""), listOf("abc"), listOf("ab", "c"),
            listOf("a", "bbb"), listOf("", "", ""), listOf("a", ""), listOf("A\uD83D\uDE0AZ"),
        )
        for (input in inputs) {
            val scanner = Scanner.of(SourceLines.of(input.map { SourceLine.of(it, null) }))
            val positions = mutableListOf<Position>()
            val characters = mutableListOf<Char>()
            val codePoints = mutableListOf<Int>()
            val previousCodePoints = mutableListOf<Int>()
            while (scanner.hasNext()) {
                positions.add(scanner.position())
                characters.add(scanner.peek())
                codePoints.add(scanner.peekCodePoint())
                previousCodePoints.add(scanner.peekPreviousCodePoint())
                scanner.next()
            }
            assertEquals(Scanner.END, scanner.peek())
            for (index in positions.indices.reversed()) {
                scanner.prev()
                assertEquals(positions[index], scanner.position(), input.toString())
                assertEquals(characters[index], scanner.peek())
                assertEquals(codePoints[index], scanner.peekCodePoint())
                assertEquals(previousCodePoints[index], scanner.peekPreviousCodePoint())
                val position = scanner.position()
                scanner.next()
                scanner.prev()
                assertEquals(position, scanner.position())
            }
            val start = scanner.position()
            scanner.prev()
            assertEquals(start, scanner.position())
        }
    }

    @Test
    fun prevReversesRepeatedNextAtEof() {
        for (input in listOf(emptyList(), listOf(""), listOf("a"), listOf("a", ""))) {
            val scanner = Scanner.of(SourceLines.of(input.map { SourceLine.of(it, null) }))
            while (scanner.hasNext()) scanner.next()
            val eof = scanner.position()
            val positions = mutableListOf(eof)
            repeat(3) {
                scanner.next()
                positions.add(scanner.position())
                assertFalse(scanner.hasNext())
                assertEquals(Scanner.END, scanner.peek())
            }
            for (position in positions.dropLast(1).asReversed()) {
                scanner.prev()
                assertEquals(position, scanner.position())
                assertEquals(Scanner.END, scanner.peek())
            }
            repeat(3) { scanner.next() }
            assertEquals(-1, scanner.findPrev(Scanner.END))
            assertEquals(Position(0, 0), scanner.position())
        }
    }

    @Test
    fun findPrevIncludesCurrentPositionAndSkipsEnd() {
        val scanner = scanner("abXcdX")
        assertBackwardSearch(scanner, Position(0, 0), 'a', null, 0, Position(0, 0))
        assertBackwardSearch(scanner, Position(0, 2), 'X', null, 0, Position(0, 2))
        assertBackwardSearch(scanner, Position(0, 4), 'X', null, 2, Position(0, 2))
        assertBackwardSearch(scanner, Position(0, 6), 'X', null, 1, Position(0, 5))
        assertBackwardSearch(scanner, Position(0, 6), 'z', null, -1, Position(0, 0))
        assertBackwardSearch(scanner, Position(0, 6), Scanner.END, null, -1, Position(0, 0))
        scanner.setPosition(Position(0, 6))
        assertEquals(1, scanner.findPrev(CharMatcher { true }))
        assertEquals('X', scanner.peek())
        assertEquals(0, scanner.findPrev(CharMatcher { true }))

        val lines = scanner("ab", "c")
        assertBackwardSearch(lines, Position(1, 1), '\n', null, 2, Position(0, 2))
        assertBackwardSearch(lines, Position(0, 2), '\n', null, 0, Position(0, 2))
    }

    @Test
    fun findPrevHonorsInclusiveBounds() {
        val scanner = scanner("aXbYc")
        assertBackwardSearch(scanner, Position(0, 5), 'Y', Position(0, 1), 2, Position(0, 3))
        assertBackwardSearch(scanner, Position(0, 5), 'Y', Position(0, 3), 2, Position(0, 3))
        assertBackwardSearch(scanner, Position(0, 5), 'X', Position(0, 3), -1, Position(0, 3))
        assertBackwardSearch(scanner, Position(0, 5), 'z', Position(0, 3), -1, Position(0, 3))
        assertBackwardSearch(scanner, Position(0, 3), 'Y', Position(0, 3), 0, Position(0, 3))
        assertBackwardSearch(scanner, Position(0, 3), 'X', Position(0, 3), -1, Position(0, 3))
        assertBackwardSearch(scanner, Position(0, 5), 'c', Position(0, 5), -1, Position(0, 5))
        assertBackwardSearch(scanner, Position(0, 0), 'a', Position(0, 0), 0, Position(0, 0))

        val lines = scanner("aXb", "cYd")
        assertBackwardSearch(lines, Position(1, 3), 'Y', Position(0, 3), 2, Position(1, 1))
        assertBackwardSearch(lines, Position(1, 3), '\n', Position(0, 0), 4, Position(0, 3))
        assertBackwardSearch(lines, Position(1, 3), '\n', Position(0, 3), 4, Position(0, 3))
        assertBackwardSearch(lines, Position(1, 3), 'X', Position(0, 3), -1, Position(0, 3))
        assertBackwardSearch(lines, Position(1, 3), 'a', Position(1, 0), -1, Position(1, 0))
        assertBackwardSearch(lines, Position(1, 3), 'a', Position(0, 0), 7, Position(0, 0))
    }

    @Test
    fun invalidBoundsThrowBeforeMovementOrMatching() {
        val scanner = scanner("aXb", "cYd")
        scanner.setPosition(Position(0, 1))
        val current = scanner.position()
        for (bound in listOf(Position(0, 2), Position(1, 1), Position(-1, 0), Position(0, -1), Position(0, 4), Position(2, 0))) {
            assertFailsWith<IllegalArgumentException> { scanner.findPrev('a', bound) }
            assertFailsWith<IllegalArgumentException> {
                scanner.findPrev(CharMatcher { error("Invalid bound must be checked first") }, bound)
            }
            assertEquals(current, scanner.position())
            assertEquals('X', scanner.peek())
        }
    }

    @Test
    fun emptyInputAcceptsOnlyCanonicalBoundAndNeverCallsMatcher() {
        for (scanner in listOf(scanner(), scanner(""))) {
            val start = scanner.position()
            val matcher = CharMatcher { error("END must not be passed to the matcher") }
            assertEquals(-1, scanner.findPrev('a'))
            assertEquals(-1, scanner.findPrev(Scanner.END))
            assertEquals(-1, scanner.findPrev(matcher))
            assertEquals(-1, scanner.findPrev('a', start))
            assertEquals(-1, scanner.findPrev(Scanner.END, start))
            assertEquals(-1, scanner.findPrev(matcher, start))
            for (bound in listOf(Position(0, 1), Position(1, 0), Position(-1, 0), Position(0, -1))) {
                assertFailsWith<IllegalArgumentException> { scanner.findPrev('a', bound) }
                assertFailsWith<IllegalArgumentException> { scanner.findPrev(matcher, bound) }
            }
            assertEquals(start, scanner.position())
        }
    }

    @Test
    fun findPrevCountsUtf16UnitsAndPreservesSourceSpans() {
        val unicode = scanner("A\uD83D\uDE0AZ")
        assertBackwardSearch(unicode, Position(0, 4), '\uDE0A', null, 2, Position(0, 2))
        assertEquals('\uDE0A'.code, unicode.peekCodePoint())
        assertEquals('\uD83D'.code, unicode.peekPreviousCodePoint())
        assertBackwardSearch(unicode, Position(0, 4), '\uD83D', null, 3, Position(0, 1))
        assertEquals(128522, unicode.peekCodePoint())
        assertEquals('A'.code, unicode.peekPreviousCodePoint())
        unicode.setPosition(Position(0, 3))
        assertEquals(128522, unicode.peekPreviousCodePoint())

        val scanner = Scanner(listOf(
            SourceLine.of("ab", SourceSpan.of(10, 3, 13, 2)),
            SourceLine.of("cde", SourceSpan.of(11, 4, 20, 3)),
        ), 1, 3)
        val end = scanner.position()
        assertEquals(5, scanner.findPrev('b'))
        assertSourceLines(scanner.getSource(scanner.position(), end), "b\ncde",
            SourceSpan.of(10, 4, 14, 1), SourceSpan.of(11, 4, 20, 3))
    }

    @Test
    fun next() {
        val scanner = Scanner(listOf(SourceLine.of("foo bar", null)), 0, 4)
        assertEquals('b', scanner.peek())
        scanner.next()
        assertEquals('a', scanner.peek())
        scanner.next()
        assertEquals('r', scanner.peek())
        scanner.next()
        assertEquals(Scanner.END, scanner.peek())
    }

    @Test
    fun multipleLines() {
        val scanner = Scanner(
            listOf(SourceLine.of("ab", null), SourceLine.of("cde", null)),
            0,
            0,
        )
        assertTrue(scanner.hasNext())
        assertEquals(Scanner.END.code, scanner.peekPreviousCodePoint())
        assertEquals('a', scanner.peek())
        scanner.next()

        assertTrue(scanner.hasNext())
        assertEquals('a'.code, scanner.peekPreviousCodePoint())
        assertEquals('b', scanner.peek())
        scanner.next()

        assertTrue(scanner.hasNext())
        assertEquals('b'.code, scanner.peekPreviousCodePoint())
        assertEquals('\n', scanner.peek())
        scanner.next()

        assertTrue(scanner.hasNext())
        assertEquals('\n'.code, scanner.peekPreviousCodePoint())
        assertEquals('c', scanner.peek())
        scanner.next()

        assertTrue(scanner.hasNext())
        assertEquals('c'.code, scanner.peekPreviousCodePoint())
        assertEquals('d', scanner.peek())
        scanner.next()

        assertTrue(scanner.hasNext())
        assertEquals('d'.code, scanner.peekPreviousCodePoint())
        assertEquals('e', scanner.peek())
        scanner.next()

        assertFalse(scanner.hasNext())
        assertEquals('e'.code, scanner.peekPreviousCodePoint())
        assertEquals(Scanner.END, scanner.peek())
    }

    @Test
    fun codePoints() {
        val scanner = Scanner(listOf(SourceLine.of("\uD83D\uDE0A", null)), 0, 0)

        assertTrue(scanner.hasNext())
        assertEquals(Scanner.END.code, scanner.peekPreviousCodePoint())
        assertEquals(128522, scanner.peekCodePoint())
        scanner.next()
        scanner.next()

        assertFalse(scanner.hasNext())
        assertEquals(128522, scanner.peekPreviousCodePoint())
        assertEquals(Scanner.END.code, scanner.peekCodePoint())
    }

    @Test
    fun textBetween() {
        val scanner = Scanner(
            listOf(
                SourceLine.of("ab", SourceSpan.of(10, 3, 13, 2)),
                SourceLine.of("cde", SourceSpan.of(11, 4, 20, 3)),
            ),
            0,
            0,
        )

        val start = scanner.position()

        scanner.next()
        assertSourceLines(
            scanner.getSource(start, scanner.position()),
            "a",
            SourceSpan.of(10, 3, 13, 1),
        )

        val afterA = scanner.position()

        scanner.next()
        assertSourceLines(
            scanner.getSource(start, scanner.position()),
            "ab",
            SourceSpan.of(10, 3, 13, 2),
        )

        val afterB = scanner.position()

        scanner.next()
        assertSourceLines(
            scanner.getSource(start, scanner.position()),
            "ab\n",
            SourceSpan.of(10, 3, 13, 2),
        )

        scanner.next()
        assertSourceLines(
            scanner.getSource(start, scanner.position()),
            "ab\nc",
            SourceSpan.of(10, 3, 13, 2),
            SourceSpan.of(11, 4, 20, 1),
        )

        scanner.next()
        assertSourceLines(
            scanner.getSource(start, scanner.position()),
            "ab\ncd",
            SourceSpan.of(10, 3, 13, 2),
            SourceSpan.of(11, 4, 20, 2),
        )

        scanner.next()
        assertSourceLines(
            scanner.getSource(start, scanner.position()),
            "ab\ncde",
            SourceSpan.of(10, 3, 13, 2),
            SourceSpan.of(11, 4, 20, 3),
        )

        assertSourceLines(
            scanner.getSource(afterA, scanner.position()),
            "b\ncde",
            SourceSpan.of(10, 4, 14, 1),
            SourceSpan.of(11, 4, 20, 3),
        )

        assertSourceLines(
            scanner.getSource(afterB, scanner.position()),
            "\ncde",
            SourceSpan.of(11, 4, 20, 3),
        )
    }

    @Test
    fun nextString() {
        val scanner = Scanner.of(
            SourceLines.of(
                listOf(
                    SourceLine.of("hey ya", null),
                    SourceLine.of("hi", null),
                ),
            ),
        )
        assertFalse(scanner.next("hoy"))
        assertTrue(scanner.next("hey"))
        assertTrue(scanner.next(' '))
        assertFalse(scanner.next("yo"))
        assertTrue(scanner.next("ya"))
        assertFalse(scanner.next(" "))
    }

    @Test
    fun matchingAndFinding() {
        val scanner = Scanner.of(SourceLines.of(SourceLine.of("  aaa,b", null)))
        assertEquals(2, scanner.whitespace())
        assertEquals(3, scanner.matchMultiple('a'))
        assertEquals(0, scanner.find(','))
        assertTrue(scanner.next(','))
        assertEquals(0, scanner.find(CharMatcher { it == 'b' }))
        assertEquals(1, scanner.match(CharMatcher { it == 'b' }))
        assertFalse(scanner.hasNext())
    }

    private fun scanner(vararg lines: String): Scanner =
        Scanner.of(SourceLines.of(lines.map { SourceLine.of(it, null) }))

    private fun assertBackwardSearch(
        scanner: Scanner,
        start: Position,
        target: Char,
        bound: Position?,
        expectedCount: Int,
        expectedPosition: Position,
    ) {
        scanner.setPosition(start)
        assertEquals(expectedCount, if (bound == null) scanner.findPrev(target) else scanner.findPrev(target, bound))
        assertEquals(expectedPosition, scanner.position())
        scanner.setPosition(start)
        val matcher = CharMatcher {
            assertNotEquals(Scanner.END, it)
            it == target
        }
        assertEquals(expectedCount, if (bound == null) scanner.findPrev(matcher) else scanner.findPrev(matcher, bound))
        assertEquals(expectedPosition, scanner.position())
    }

    private fun assertSourceLines(
        sourceLines: SourceLines,
        expectedContent: String,
        vararg expectedSourceSpans: SourceSpan,
    ) {
        assertEquals(expectedContent, sourceLines.content)
        assertEquals(expectedSourceSpans.toList(), sourceLines.sourceSpans)
    }
}
