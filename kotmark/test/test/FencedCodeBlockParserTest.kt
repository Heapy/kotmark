package io.heapy.kotmark.test

import io.heapy.kotmark.node.FencedCodeBlock
import io.heapy.kotmark.parser.parser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FencedCodeBlockParserTest : CoreRenderingTestCase() {
    @Test
    fun backtickInfoCanStartWithTilde() {
        val document = parser().parse("```~/.bashrc\ncode\n```\n")
        val codeBlock = assertIs<FencedCodeBlock>(document.firstChild)
        assertEquals("`", codeBlock.fenceCharacter)
        assertEquals(3, codeBlock.openingFenceLength)
        assertEquals("~/.bashrc", codeBlock.info)
        assertEquals("code\n", codeBlock.literal)
    }

    @Test
    fun tildeInfoCanStartWithBacktick() {
        assertRendering(
            "~~~`info`\ncode\n~~~\n",
            "<pre><code class=\"language-`info`\">code\n</code></pre>\n",
        )
    }
}
