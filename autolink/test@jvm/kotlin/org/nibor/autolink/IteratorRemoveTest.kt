// Port of autolink-java 0.12.0 tests. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNCHECKED_CAST")
package org.nibor.autolink

import kotlin.test.Test
import kotlin.test.assertFailsWith

// Kotlin read-only Iterator has no remove. Verify the Java-facing contract on JVM.
class IteratorRemoveTest {
    @Test fun linkIterableRemoveUnsupported() {
        val iterator = LinkExtractor.builder().build().extractLinks("foo http://example.com").iterator()
        assertFailsWith<UnsupportedOperationException> { (iterator as java.util.Iterator<LinkSpan>).remove() }
    }
    @Test fun spanIterableRemoveUnsupported() {
        val iterator = LinkExtractor.builder().build().extractSpans("test").iterator()
        assertFailsWith<UnsupportedOperationException> { (iterator as java.util.Iterator<Span>).remove() }
    }
}
