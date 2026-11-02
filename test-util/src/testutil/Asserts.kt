package io.heapy.kotmark.testutil

object Asserts {
    fun assertRendering(
        source: String,
        expectedRendering: String,
        actualRendering: String,
    ) {
        val expected = showTabs("$expectedRendering\n\n$source")
        val actual = showTabs("$actualRendering\n\n$source")
        if (actual != expected) {
            throw AssertionError("Expected:\n$expected\nActual:\n$actual")
        }
    }

    private fun showTabs(value: String): String = value.replace('\t', '\u2192')
}
