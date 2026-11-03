package io.heapy.kotmark.configuration

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ExtensionsBuilder
import io.heapy.kotmark.extensions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ExtensionsBuilderTest {
    @Test
    fun snapshotsRegistrationsButSharesExtensionObjects() {
        val first = object : Extension {}
        val second = object : Extension {}
        val source = mutableListOf(second, first)
        lateinit var builder: ExtensionsBuilder
        val result = extensions {
            builder = this
            add(first)
            addAll(source)
            source.clear()
        }
        builder.add(second)
        builder.addAll(listOf(second))

        assertEquals(3, result.size)
        assertSame(first, result[0])
        assertSame(second, result[1])
        assertSame(first, result[2])
        assertTrue(extensions {}.isEmpty())
    }
}
