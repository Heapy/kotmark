package io.heapy.kotmark.publication

import io.heapy.kotmark.ext.math.InlineMath
import io.heapy.kotmark.ext.math.MathExtension
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WithoutRendererTest {
    @Test
    fun parserAndExistingRendererDoNotNeedOptionalClasses() {
        val math = MathExtension.create()
        val document = parser { extensions(listOf(math)) }.parse("\$x<y\$")
        assertEquals("x<y", assertIs<InlineMath>(document.firstChild?.firstChild).literal)
        assertEquals(
            "<p><span class=\"math math-inline\">x&lt;y</span></p>\n",
            htmlRenderer { extensions(listOf(math)) }.render(document),
        )
    }
}
