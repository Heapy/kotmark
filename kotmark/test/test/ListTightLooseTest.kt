package io.heapy.kotmark.test

import kotlin.test.Test

class ListTightLooseTest : CoreRenderingTestCase() {
    @Test
    fun tight() {
        assertRendering(
            "- foo\n" +
                "- bar\n" +
                "+ baz\n",
            """
                |<ul>
                |<li>foo</li>
                |<li>bar</li>
                |</ul>
                |<ul>
                |<li>baz</li>
                |</ul>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun loose() {
        assertRendering(
            """
                |- foo
                |
                |- bar
                |
                |
                |- baz
                |
            """.trimMargin(),
            """
                |<ul>
                |<li>
                |<p>foo</p>
                |</li>
                |<li>
                |<p>bar</p>
                |</li>
                |<li>
                |<p>baz</p>
                |</li>
                |</ul>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun looseNested() {
        assertRendering(
            """
                |- foo
                |  - bar
                |
                |
                |    baz
            """.trimMargin(),
            """
                |<ul>
                |<li>foo
                |<ul>
                |<li>
                |<p>bar</p>
                |<p>baz</p>
                |</li>
                |</ul>
                |</li>
                |</ul>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun looseNested2() {
        assertRendering(
            """
                |- a
                |  - b
                |
                |    c
                |- d
                |
            """.trimMargin(),
            """
                |<ul>
                |<li>a
                |<ul>
                |<li>
                |<p>b</p>
                |<p>c</p>
                |</li>
                |</ul>
                |</li>
                |<li>d</li>
                |</ul>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun looseOuter() {
        assertRendering(
            """
                |- foo
                |  - bar
                |
                |
                |  baz
            """.trimMargin(),
            """
                |<ul>
                |<li>
                |<p>foo</p>
                |<ul>
                |<li>bar</li>
                |</ul>
                |<p>baz</p>
                |</li>
                |</ul>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun looseListItem() {
        assertRendering(
            "- one\n" +
                "\n" +
                "  two\n",
            """
                |<ul>
                |<li>
                |<p>one</p>
                |<p>two</p>
                |</li>
                |</ul>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun tightWithBlankLineAfter() {
        assertRendering(
            "- foo\n" +
                "- bar\n" +
                "\n",
            """
                |<ul>
                |<li>foo</li>
                |<li>bar</li>
                |</ul>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun tightListWithCodeBlock() {
        assertRendering(
            """
                |- a
                |- ```
                |  b
                |
                |
                |  ```
                |- c
                |
            """.trimMargin(),
            """
                |<ul>
                |<li>a</li>
                |<li>
                |<pre><code>b
                |
                |
                |</code></pre>
                |</li>
                |<li>c</li>
                |</ul>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun tightListWithCodeBlock2() {
        assertRendering(
            """
                |* foo
                |  ```
                |  bar
                |
                |  ```
                |  baz
                |
            """.trimMargin(),
            """
                |<ul>
                |<li>foo
                |<pre><code>bar
                |
                |</code></pre>
                |baz</li>
                |</ul>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun looseEmptyListItem() {
        assertRendering(
            "* a\n" +
                "*\n" +
                "\n" +
                "* c",
            """
                |<ul>
                |<li>
                |<p>a</p>
                |</li>
                |<li></li>
                |<li>
                |<p>c</p>
                |</li>
                |</ul>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun looseBlankLineAfterCodeBlock() {
        assertRendering(
            """
                |1. ```
                |   foo
                |   ```
                |
                |   bar
            """.trimMargin(),
            """
                |<ol>
                |<li>
                |<pre><code>foo
                |</code></pre>
                |<p>bar</p>
                |</li>
                |</ol>
                |
            """.trimMargin(),
        )
    }
}
