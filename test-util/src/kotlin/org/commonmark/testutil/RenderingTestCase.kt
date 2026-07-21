package org.commonmark.testutil

abstract class RenderingTestCase {
    protected abstract fun render(source: String): String

    protected fun assertRendering(source: String, expectedResult: String) {
        Asserts.assertRendering(source, expectedResult, render(source))
    }
}
