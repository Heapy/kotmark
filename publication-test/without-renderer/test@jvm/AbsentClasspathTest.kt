package io.heapy.kotmark.publication

import io.heapy.kotmark.ext.math.MathExtension
import kotlin.test.Test
import kotlin.test.assertNull

class AbsentClasspathTest {
    @Test
    fun htmlAndUnselectedExtensionsAreAbsent() {
        val loader = MathExtension::class.java.classLoader
        assertNull(loader.getResource("io/heapy/kotmark/ext/highlight/HighlightExtension.class"))
        assertNull(loader.getResource("io/heapy/kotmark/renderer/kotlinxhtml/KotlinxHtmlAdapter.class"))
        assertNull(loader.getResource("kotlinx/html/FlowContent.class"))
    }
}
