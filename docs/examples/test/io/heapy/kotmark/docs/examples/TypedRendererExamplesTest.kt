package io.heapy.kotmark.docs.examples

import io.heapy.kotmark.docs.examples.extensions.customCodeRenderer
import io.heapy.kotmark.parser.parser
import kotlin.test.Test
import kotlin.test.assertEquals

class TypedRendererExamplesTest {
    @Test
    fun customKotlinxHandlerPreservesEscaping() {
        assertEquals(
            "<p><code class=\"inline-code\">x&lt;y</code></p>",
            customCodeRenderer().render(parser().parse("`x<y`")),
        )
    }
}
