package io.heapy.kotmark.test

import io.heapy.kotmark.node.AbstractVisitor
import io.heapy.kotmark.node.Delimited
import io.heapy.kotmark.node.Emphasis
import io.heapy.kotmark.node.StrongEmphasis
import io.heapy.kotmark.parser.Parser
import kotlin.test.Test
import kotlin.test.assertEquals

class DelimitedTest {
    @Test
    fun emphasisDelimiters() {
        val input =
            "* *emphasis* \n" +
                "* **strong** \n" +
                "* _important_ \n" +
                "* __CRITICAL__ \n"

        val document = Parser.builder().build().parse(input)

        val list = mutableListOf<Delimited>()
        document.accept(
            object : AbstractVisitor() {
                override fun visit(emphasis: Emphasis) {
                    list.add(emphasis)
                }

                override fun visit(strongEmphasis: StrongEmphasis) {
                    list.add(strongEmphasis)
                }
            },
        )

        assertEquals(4, list.size)

        val emphasis = list[0]
        val strong = list[1]
        val important = list[2]
        val critical = list[3]

        assertEquals("*", emphasis.openingDelimiter)
        assertEquals("*", emphasis.closingDelimiter)
        assertEquals("**", strong.openingDelimiter)
        assertEquals("**", strong.closingDelimiter)
        assertEquals("_", important.openingDelimiter)
        assertEquals("_", important.closingDelimiter)
        assertEquals("__", critical.openingDelimiter)
        assertEquals("__", critical.closingDelimiter)
    }
}
