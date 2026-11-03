package io.heapy.kotmark.publication

import io.heapy.kotmark.ext.math.MathExtension
import io.heapy.kotmark.renderer.kotlinxhtml.adapters.mathAdapter
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.kotlinxhtml.kotlinxHtmlRenderer
import kotlinx.html.div
import kotlinx.html.stream.createHTML
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WithRendererTest {
    private val parser = parser { extensions(listOf(MathExtension.create())) }

    @Test
    fun explicitlyRegisteredAdapterComposesWithHtmlDslAndEscapesSource() {
        val document = parser.parse("Before \$x<y & z\$ after.")
        val renderer = kotlinxHtmlRenderer { adapters(mathAdapter()) }
        val html = createHTML(prettyPrint = false).div {
            renderer.render(document, this)
        }
        assertEquals(
            "<div><p>Before <span class=\"math math-inline\">x&lt;y &amp; z</span> after.</p></div>",
            html,
        )
    }

    @Test
    fun blockMathUsesBlockMarkup() {
        val document = parser.parse("\$\$\nx<y\n\$\$")
        val renderer = kotlinxHtmlRenderer { adapters(mathAdapter()) }
        assertEquals(
            "<div><div class=\"math math-block\">x&lt;y\n</div></div>",
            createHTML(prettyPrint = false).div { renderer.render(document, this) },
        )
    }

    @Test
    fun adapterIsNotAutomaticallyRegistered() {
        assertFailsWith<IllegalStateException> {
            createHTML(prettyPrint = false).div {
                kotlinxHtmlRenderer().render(parser.parse("\$x\$"), this)
            }
        }
    }
}
