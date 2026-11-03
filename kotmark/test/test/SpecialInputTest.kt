package io.heapy.kotmark.test

import kotlin.test.Test

class SpecialInputTest : CoreRenderingTestCase() {
    @Test
    fun listCannotInterruptParagraphWithOnlyLinkReferenceDefinitions() {
        // Reference definitions leave the paragraph open, so ordinary interruption rules apply.
        assertRendering(
            "[foo]: /url\n2. a\n\n[foo]",
            "<p>2. a</p>\n<p><a href=\"/url\">foo</a></p>\n",
        )
        assertRendering("[foo]: /url\n-\n\n[foo]", "<p>-</p>\n<p><a href=\"/url\">foo</a></p>\n")
        assertRendering("[foo]: /url \"t\"\n2. a", "<p>2. a</p>\n")
        assertRendering("> [foo]: /url\n> 2. a", "<blockquote>\n<p>2. a</p>\n</blockquote>\n")
        assertRendering("[foo]: /url\n1. a", "<ol>\n<li>a</li>\n</ol>\n")
        assertRendering("[foo]: /url\n- a", "<ul>\n<li>a</li>\n</ul>\n")
    }

    @Test
    fun listMayStartAfterBlankLineFollowingReferenceDefinition() {
        assertRendering(
            "[foo]: /url\n\n2. a\n\n[foo]",
            "<ol start=\"2\">\n<li>a</li>\n</ol>\n<p><a href=\"/url\">foo</a></p>\n",
        )
    }

    @Test
    fun linkReferenceDefinitionWithUnbalancedParens() {
        assertRendering("[foo]: (", "<p>[foo]: (</p>\n")
        assertRendering("[foo]: (\nbar", "<p>[foo]: (\nbar</p>\n")
        assertRendering("[foo]: a(b\n\n[foo]", "<p>[foo]: a(b</p>\n<p>[foo]</p>\n")
        assertRendering(
            "[foo]: a(b) \"t\"\n\n[foo]",
            "<p><a href=\"a(b)\" title=\"t\">foo</a></p>\n",
        )
    }

    @Test
    fun codeSpansAfterUnmatchedBackticks() {
        assertRendering("`` unmatched `matched`", "<p>`` unmatched <code>matched</code></p>\n")
        assertRendering("`` unmatched\n`a\nb`", "<p>`` unmatched\n<code>a b</code></p>\n")
        assertRendering("`a ``b`` c ``", "<p>`a <code>b</code> c ``</p>\n")
        assertRendering("`a ``b`` c ``d``", "<p>`a <code>b</code> c <code>d</code></p>\n")
    }

    @Test
    fun backtickCacheIsIsolatedBetweenParagraphsAndParses() {
        assertRendering("x `unclosed\n\n`code`", "<p>x `unclosed</p>\n<p><code>code</code></p>\n")
        assertRendering("x `unclosed", "<p>x `unclosed</p>\n")
        assertRendering("`code`", "<p><code>code</code></p>\n")
    }

    @Test
    fun autolinksRecoverAfterNestedOpeningBrackets() {
        assertRendering(
            "<<https://example.com>",
            "<p>&lt;<a href=\"https://example.com\">https://example.com</a></p>\n",
        )
        assertRendering(
            "<bad <me@example.com>>",
            "<p>&lt;bad <a href=\"mailto:me@example.com\">me@example.com</a>&gt;</p>\n",
        )
        assertRendering(
            "x <bad<em>ok</em> <https://bad space> <https://example.com>",
            "<p>x &lt;bad<em>ok</em> &lt;https://bad space&gt; " +
                "<a href=\"https://example.com\">https://example.com</a></p>\n",
        )
    }

    @Test
    fun empty() {
        assertRendering("", "")
    }

    @Test
    fun nullCharacterShouldBeReplaced() {
        assertRendering("foo\u0000bar", "<p>foo�bar</p>\n")
    }

    @Test
    fun nullCharacterEntityShouldBeReplaced() {
        assertRendering("foo&#0;bar", "<p>foo�bar</p>\n")
    }

    @Test
    fun crLfAsLineSeparatorShouldBeParsed() {
        assertRendering("foo\r\nbar", "<p>foo\nbar</p>\n")
    }

    @Test
    fun crLfAtEndShouldBeParsed() {
        assertRendering("foo\r\n", "<p>foo</p>\n")
    }

    @Test
    fun mixedLineSeparators() {
        assertRendering("- a\n- b\r- c\r\n- d", "<ul>\n<li>a</li>\n<li>b</li>\n<li>c</li>\n<li>d</li>\n</ul>\n")
        assertRendering("a\n\nb\r\rc\r\n\r\nd\n\re", "<p>a</p>\n<p>b</p>\n<p>c</p>\n<p>d</p>\n<p>e</p>\n")
    }

    @Test
    fun surrogatePair() {
        assertRendering("surrogate pair: 𝄞", "<p>surrogate pair: 𝄞</p>\n")
    }

    @Test
    fun surrogatePairInLinkDestination() {
        assertRendering("[title](𝄞)", """<p><a href="𝄞">title</a></p>""" + "\n")
    }

    @Test
    fun indentedCodeBlockWithMixedTabsAndSpaces() {
        assertRendering("    foo\n\tbar", "<pre><code>foo\nbar\n</code></pre>\n")
    }

    @Test
    fun partiallyConsumedTabBeforeHtmlBlockStart() {
        assertRendering(">\t<div>", "<blockquote>\n  <div>\n</blockquote>\n")
    }

    @Test
    fun partiallyConsumedTabBeforeHtmlBlockContinuation() {
        assertRendering("> <div>\n>\tfoo", "<blockquote>\n<div>\n  foo\n</blockquote>\n")
    }

    @Test
    fun tightListInBlockQuote() {
        assertRendering("> *\n> * a", "<blockquote>\n<ul>\n<li></li>\n<li>a</li>\n</ul>\n</blockquote>\n")
    }

    @Test
    fun looseListInBlockQuote() {
        // Second line in block quote is considered blank for purpose of loose list
        assertRendering("> *\n>\n> * a", "<blockquote>\n<ul>\n<li></li>\n<li>\n<p>a</p>\n</li>\n</ul>\n</blockquote>\n")
    }

    @Test
    fun lineWithOnlySpacesAfterListBullet() {
        assertRendering("-  \n  \n  foo\n", "<ul>\n<li></li>\n</ul>\n<p>foo</p>\n")
    }

    @Test
    fun listWithTwoSpacesForFirstBullet() {
        // We have two spaces after the bullet, but no content. With content, the next line would be required
        assertRendering("*  \n  foo\n", "<ul>\n<li>foo</li>\n</ul>\n")
    }

    @Test
    fun orderedListMarkerOnly() {
        assertRendering("2.", "<ol start=\"2\">\n<li></li>\n</ol>\n")
    }

    @Test
    fun columnIsInTabOnPreviousLine() {
        assertRendering(
            "- foo\n\n\tbar\n\n# baz\n",
            """
                |<ul>
                |<li>
                |<p>foo</p>
                |<p>bar</p>
                |</li>
                |</ul>
                |<h1>baz</h1>
                |
            """.trimMargin(),
        )
        assertRendering(
            "- foo\n\n\tbar\n# baz\n",
            """
                |<ul>
                |<li>
                |<p>foo</p>
                |<p>bar</p>
                |</li>
                |</ul>
                |<h1>baz</h1>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun linkLabelWithBracket() {
        assertRendering("[a[b]\n\n[a[b]: /", "<p>[a[b]</p>\n<p>[a[b]: /</p>\n")
        assertRendering("[a]b]\n\n[a]b]: /", "<p>[a]b]</p>\n<p>[a]b]: /</p>\n")
        assertRendering("[a[b]]\n\n[a[b]]: /", "<p>[a[b]]</p>\n<p>[a[b]]: /</p>\n")
    }

    @Test
    fun linkLabelLength() {
        val label1 = "a".repeat(999)
        assertRendering("[foo][" + label1 + "]\n\n[" + label1 + "]: /", """<p><a href="/">foo</a></p>""" + "\n")
        assertRendering(
            "[foo][x" + label1 + "]\n\n[x" + label1 + "]: /",
            "<p>[foo][x" + label1 + "]</p>\n<p>[x" + label1 + "]: /</p>\n",
        )
        assertRendering(
            "[foo][\n" + label1 + "]\n\n[\n" + label1 + "]: /",
            "<p>[foo][\n" + label1 + "]</p>\n<p>[\n" + label1 + "]: /</p>\n",
        )

        val label2 = "a\n".repeat(499)
        assertRendering("[foo][" + label2 + "]\n\n[" + label2 + "]: /", """<p><a href="/">foo</a></p>""" + "\n")
        assertRendering(
            "[foo][12" + label2 + "]\n\n[12" + label2 + "]: /",
            "<p>[foo][12" + label2 + "]</p>\n<p>[12" + label2 + "]: /</p>\n",
        )
    }

    @Test
    fun linkDestinationEscaping() {
        // Backslash escapes `)`
        assertRendering("""[foo](\))""", """<p><a href=")">foo</a></p>""" + "\n")
        // ` ` is not escapable, so the backslash is a literal backslash and there's an optional space at the end
        assertRendering("""[foo](\ )""", """<p><a href="\">foo</a></p>""" + "\n")
        // Backslash is a literal, so valid
        assertRendering("""[foo](<a\b>)""", """<p><a href="a\b">foo</a></p>""" + "\n")
        // Backslash escapes `>` but there's another `>`, valid
        assertRendering("""[foo](<a\>>)""", """<p><a href="a&gt;">foo</a></p>""" + "\n")

        // This is a tricky one. There's `<` so we try to parse it as a `<` link but fail.
        assertRendering("""[foo](<\>)""", "<p>[foo](&lt;&gt;)</p>\n")
    }

    // commonmark/CommonMark#468
    @Test
    fun linkReferenceBackslash() {
        // Backslash escapes ']', so not a valid link label
        assertRendering("""[\]: test""", "<p>[]: test</p>\n")
        // Backslash is a literal, so valid
        assertRendering("[a\\b]\n\n[a\\b]: test", """<p><a href="test">a\b</a></p>""" + "\n")
        // Backslash escapes `]` but there's another `]`, valid
        assertRendering("[a\\]]\n\n[a\\]]: test", """<p><a href="test">a]</a></p>""" + "\n")
    }

    // commonmark/cmark#177
    @Test
    fun emphasisMultipleOf3Rule() {
        assertRendering("a***b* c*", "<p>a*<em><em>b</em> c</em></p>\n")
    }

    @Test
    fun emphasis() {
        assertRendering("*****Hello*world****", "<p>**<em><strong>Hello<em>world</em></strong></em></p>\n")
    }

    @Test
    fun renderEvenRegexpProducesStackoverflow() {
        // Regression check: completing the render without a stack overflow is the assertion.
        val _ = render("""Contents: <!--[if gte mso 9]> <w:LatentStyles DefLockedState="false" DefUnhideWhenUsed="false" DefSemiHidden="false" DefQFormat="false" DefPriority="99" LatentStyleCount="371">  <w:xxx Locked="false" Priority="52" Name="Grid Table 7 Colorful 6"/> <w:xxx Locked="false" Priority="46" Name="List Table 1 Light"/> <w:xxx Locked="false" Priority="47" Name="List Table 2"/> <w:xxx Locked="false" Priority="48" Name="List Table 3"/> <w:xxx Locked="false" Priority="49" Name="List Table 4"/> <w:xxx Locked="false" Priority="50" Name="List Table 5 Dark"/> <w:xxx Locked="false" Priority="51" Name="List Table 6 Colorful"/> <w:xxx Locked="false" Priority="52" Name="List Table 7 Colorful"/> <w:xxx Locked="false" Priority="46" Name="List Table 1 Light Accent 1"/> <w:xxx Locked="false" Priority="47" Name="List Table 2 Accent 1"/> <w:xxx Locked="false" Priority="48" Name="List Table 3 Accent 1"/> <w:xxx Locked="false" Priority="49" Name="List Table 4 Accent 1"/> <w:xxx Locked="false" Priority="50" Name="List Table 5 Dark Accent 1"/>  <w:xxx Locked="false" Priority="52" Name="List Table 7 Colorful Accent 1"/> <w:xxx Locked="false" Priority="46" Name="List Table 1 Light Accent 2"/> <w:xxx Locked="false" Priority="47" Name="List Table 2 Accent 2"/> <w:xxx Locked="false" Priority="48" Name="List Table 3 Accent 2"/> <w:xxx Locked="false" Priority="49" Name="List Table 4 Accent 2"/> <w:xxx Locked="false" Priority="50" Name="List Table 5 Dark Accent 2"/> <w:xxx Locked="false" Priority="51" Name="List Table 6 Colorful Accent 2"/> <w:xxx Locked="false" Priority="52" Name="List Table 7 Colorful Accent 2"/> <w:xxx Locked="false" Priority="46" Name="List Table 1 Light Accent 3"/> <w:xxx Locked="false" Priority="47" Name="List Table 2 Accent 3"/> <w:xxx Locked="false" Priority="48" Name="List Table 3 Accent 3"/> <w:xxx Locked="false" Priority="49" Name="List Table 4 Accent 3" /> <w:xxx Locked="false" Priority="50" Name="List Table 5 Dark Accent 3"/><w:xxx Locked="false" Priority="51" Name="List Table 6 Colorful Accent 3"/></xml>""")
    }

    @Test
    fun deeplyIndentedList() {
        assertRendering(
            """
                |* one
                |  * two
                |    * three
                |      * four
            """.trimMargin(),
            """
                |<ul>
                |<li>one
                |<ul>
                |<li>two
                |<ul>
                |<li>three
                |<ul>
                |<li>four</li>
                |</ul>
                |</li>
                |</ul>
                |</li>
                |</ul>
                |</li>
                |</ul>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun trailingTabs() {
        // The tab is not treated as 4 spaces here and so does not result in a hard line break, but is just preserved.
        // This matches what commonmark.js did at the time of writing.
        assertRendering("a\t\nb\n", "<p>a\t\nb</p>\n")
    }

    @Test
    fun unicodePunctuationEmphasis() {
        // The character here is: U+12470 CUNEIFORM PUNCTUATION SIGN OLD ASSYRIAN WORD DIVIDER
        // Which is in Unicode category "Po" and needs 2 code units in UTF-16. That means to implement
        // it correctly, we need to check code points, not chars.
        // Note that currently the reference implementation doesn't implement this correctly (resulting in no <em>).
        assertRendering("foo𒑰_(bar)_", "<p>foo𒑰<em>(bar)</em></p>\n")
    }

    @Test
    fun htmlBlockInterruptingList() {
        assertRendering(
            """
                |- <script>
                |- some text
                |some other text
                |</script>
                |
            """.trimMargin(),
            """
                |<ul>
                |<li>
                |<script>
                |</li>
                |<li>some text
                |some other text
                |</script></li>
                |</ul>
                |
            """.trimMargin(),
        )

        assertRendering(
            """
                |- <script>
                |- some text
                |some other text
                |
                |</script>
                |
            """.trimMargin(),
            """
                |<ul>
                |<li>
                |<script>
                |</li>
                |<li>some text
                |some other text</li>
                |</ul>
                |</script>
                |
            """.trimMargin(),
        )
    }

    @Test
    fun emphasisAfterHardLineBreak() {
        assertRendering(
            "Hello  \n" +
                "**Bar**\n" +
                "Foo\n",
            """
                |<p>Hello<br />
                |<strong>Bar</strong>
                |Foo</p>
                |
            """.trimMargin(),
        )

        assertRendering(
            "Hello  \n" +
                "**Bar**  \n" +
                "Foo\n",
            """
                |<p>Hello<br />
                |<strong>Bar</strong><br />
                |Foo</p>
                |
            """.trimMargin(),
        )
    }
}
