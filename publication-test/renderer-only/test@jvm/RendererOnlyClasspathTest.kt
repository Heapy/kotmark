package io.heapy.kotmark.publication

import io.heapy.kotmark.renderer.kotlinxhtml.KotlinxHtmlRenderer
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RendererOnlyClasspathTest {
    @Test
    fun bothAdaptersArePackagedWithoutExtensionClasses() {
        val loader = KotlinxHtmlRenderer::class.java.classLoader
        assertNotNull(loader.getResource("io/heapy/kotmark/renderer/kotlinxhtml/adapters/MathAdapterKt.class"))
        assertNotNull(loader.getResource("io/heapy/kotmark/renderer/kotlinxhtml/adapters/HighlightAdapterKt.class"))
        assertNull(loader.getResource("io/heapy/kotmark/ext/math/MathExtension.class"))
        assertNull(loader.getResource("io/heapy/kotmark/ext/highlight/HighlightExtension.class"))
    }
}
