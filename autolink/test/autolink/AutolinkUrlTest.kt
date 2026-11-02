// Port of autolink-java 0.12.0 tests. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
package io.heapy.kotmark.autolink

import kotlin.test.Test
import kotlin.test.assertEquals

class AutolinkUrlTest : AutolinkTestCase() {
    @Test
    fun notLinkedConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertNotLinked(extractor, "")
        assertNotLinked(extractor, "foo")
        assertNotLinked(extractor, ":")
        assertNotLinked(extractor, "://")
        assertNotLinked(extractor, ":::")

    }

    @Test
    fun notLinkedConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertNotLinked(extractor, "")
        assertNotLinked(extractor, "foo")
        assertNotLinked(extractor, ":")
        assertNotLinked(extractor, "://")
        assertNotLinked(extractor, ":::")

    }

    @Test
    fun schemesConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertNotLinked(extractor, "://foo")
        assertNotLinked(extractor, "1://foo")
        assertNotLinked(extractor, "123://foo")
        assertNotLinked(extractor, "+://foo")
        assertNotLinked(extractor, "-://foo")
        assertNotLinked(extractor, ".://foo")
        assertNotLinked(extractor, "1abc://foo")
        assertLinked(extractor, LinkType.URL, "a://foo", "|a://foo|")
        assertLinked(extractor, LinkType.URL, "a123://foo", "|a123://foo|")
        assertLinked(extractor, LinkType.URL, "a123b://foo", "|a123b://foo|")
        assertLinked(extractor, LinkType.URL, "a+b://foo", "|a+b://foo|")
        assertLinked(extractor, LinkType.URL, "a-b://foo", "|a-b://foo|")
        assertLinked(extractor, LinkType.URL, "a.b://foo", "|a.b://foo|")
        assertLinked(extractor, LinkType.URL, "ABC://foo", "|ABC://foo|")

    }

    @Test
    fun schemesConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertNotLinked(extractor, "://foo")
        assertNotLinked(extractor, "1://foo")
        assertNotLinked(extractor, "123://foo")
        assertNotLinked(extractor, "+://foo")
        assertNotLinked(extractor, "-://foo")
        assertNotLinked(extractor, ".://foo")
        assertNotLinked(extractor, "1abc://foo")
        assertLinked(extractor, LinkType.URL, "a://foo", "|a://foo|")
        assertLinked(extractor, LinkType.URL, "a123://foo", "|a123://foo|")
        assertLinked(extractor, LinkType.URL, "a123b://foo", "|a123b://foo|")
        assertLinked(extractor, LinkType.URL, "a+b://foo", "|a+b://foo|")
        assertLinked(extractor, LinkType.URL, "a-b://foo", "|a-b://foo|")
        assertLinked(extractor, LinkType.URL, "a.b://foo", "|a.b://foo|")
        assertLinked(extractor, LinkType.URL, "ABC://foo", "|ABC://foo|")

    }

    @Test
    fun authorityConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "ab://", "ab://")
        assertLinked(extractor, LinkType.URL, "http://", "http://")
        assertLinked(extractor, LinkType.URL, "http:// ", "http:// ")
        assertLinked(extractor, LinkType.URL, "\"http://\"", "\"http://\"")
        assertLinked(extractor, LinkType.URL, "\"http://...\", ", "\"http://...\", ")

        assertLinked(extractor, LinkType.URL, "http://a.", "|http://a|.")

    }

    @Test
    fun authorityConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "ab://", "ab://")
        assertLinked(extractor, LinkType.URL, "http://", "http://")
        assertLinked(extractor, LinkType.URL, "http:// ", "http:// ")
        assertLinked(extractor, LinkType.URL, "\"http://\"", "\"http://\"")
        assertLinked(extractor, LinkType.URL, "\"http://...\", ", "\"http://...\", ")

        assertLinked(extractor, LinkType.URL, "http://a.", "|http://a|.")

    }

    @Test
    fun linkingConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "ab://c", "|ab://c|")
        assertLinked(extractor, LinkType.URL, "http://example.org/", "|http://example.org/|")
        assertLinked(extractor, LinkType.URL, "http://example.org/123", "|http://example.org/123|")
        assertLinked(extractor, LinkType.URL, "http://example.org/?foo=test&bar=123", "|http://example.org/?foo=test&bar=123|")
        assertLinked(extractor, LinkType.URL, "http://example.org/?foo=%20", "|http://example.org/?foo=%20|")
        assertLinked(extractor, LinkType.URL, "http://example.org/%3C", "|http://example.org/%3C|")

    }

    @Test
    fun linkingConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "ab://c", "|ab://c|")
        assertLinked(extractor, LinkType.URL, "http://example.org/", "|http://example.org/|")
        assertLinked(extractor, LinkType.URL, "http://example.org/123", "|http://example.org/123|")
        assertLinked(extractor, LinkType.URL, "http://example.org/?foo=test&bar=123", "|http://example.org/?foo=test&bar=123|")
        assertLinked(extractor, LinkType.URL, "http://example.org/?foo=%20", "|http://example.org/?foo=%20|")
        assertLinked(extractor, LinkType.URL, "http://example.org/%3C", "|http://example.org/%3C|")

    }

    @Test
    fun schemeSeparatedByNonAlphanumericConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, ".http://example.org/", ".|http://example.org/|")

    }

    @Test
    fun schemeSeparatedByNonAlphanumericConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, ".http://example.org/", ".|http://example.org/|")

    }

    @Test
    fun spaceCharactersStopUrlConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "foo http://example.org/", "foo |http://example.org/|")
        assertLinked(extractor, LinkType.URL, "http://example.org/ bar", "|http://example.org/| bar")
        assertLinked(extractor, LinkType.URL, "http://example.org/\tbar", "|http://example.org/|\tbar")
        assertLinked(extractor, LinkType.URL, "http://example.org/\nbar", "|http://example.org/|\nbar")
        assertLinked(extractor, LinkType.URL, "http://example.org/\u000Bbar", "|http://example.org/|\u000Bbar")
        assertLinked(extractor, LinkType.URL, "http://example.org/\u000Cbar", "|http://example.org/|\u000Cbar")
        assertLinked(extractor, LinkType.URL, "http://example.org/\rbar", "|http://example.org/|\rbar")

    }

    @Test
    fun spaceCharactersStopUrlConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "foo http://example.org/", "foo |http://example.org/|")
        assertLinked(extractor, LinkType.URL, "http://example.org/ bar", "|http://example.org/| bar")
        assertLinked(extractor, LinkType.URL, "http://example.org/\tbar", "|http://example.org/|\tbar")
        assertLinked(extractor, LinkType.URL, "http://example.org/\nbar", "|http://example.org/|\nbar")
        assertLinked(extractor, LinkType.URL, "http://example.org/\u000Bbar", "|http://example.org/|\u000Bbar")
        assertLinked(extractor, LinkType.URL, "http://example.org/\u000Cbar", "|http://example.org/|\u000Cbar")
        assertLinked(extractor, LinkType.URL, "http://example.org/\rbar", "|http://example.org/|\rbar")

    }

    @Test
    fun illegalCharactersStopUrlConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://example.org/<", "|http://example.org/|<")
        assertLinked(extractor, LinkType.URL, "http://example.org/>", "|http://example.org/|>")
        assertLinked(extractor, LinkType.URL, "http://example.org/<>", "|http://example.org/|<>")
        assertLinked(extractor, LinkType.URL, "http://example.org/\u0000", "|http://example.org/|\u0000")
        assertLinked(extractor, LinkType.URL, "http://example.org/\u000E", "|http://example.org/|\u000E")
        assertLinked(extractor, LinkType.URL, "http://example.org/\u007F", "|http://example.org/|\u007F")
        assertLinked(extractor, LinkType.URL, "http://example.org/\u009F", "|http://example.org/|\u009F")

    }

    @Test
    fun illegalCharactersStopUrlConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://example.org/<", "|http://example.org/|<")
        assertLinked(extractor, LinkType.URL, "http://example.org/>", "|http://example.org/|>")
        assertLinked(extractor, LinkType.URL, "http://example.org/<>", "|http://example.org/|<>")
        assertLinked(extractor, LinkType.URL, "http://example.org/\u0000", "|http://example.org/|\u0000")
        assertLinked(extractor, LinkType.URL, "http://example.org/\u000E", "|http://example.org/|\u000E")
        assertLinked(extractor, LinkType.URL, "http://example.org/\u007F", "|http://example.org/|\u007F")
        assertLinked(extractor, LinkType.URL, "http://example.org/\u009F", "|http://example.org/|\u009F")

    }

    @Test
    fun delimiterAtEndConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://example.org/.", "|http://example.org/|.")
        assertLinked(extractor, LinkType.URL, "http://example.org/..", "|http://example.org/|..")
        assertLinked(extractor, LinkType.URL, "http://example.org/,", "|http://example.org/|,")
        assertLinked(extractor, LinkType.URL, "http://example.org/:", "|http://example.org/|:")
        assertLinked(extractor, LinkType.URL, "http://example.org/?", "|http://example.org/|?")
        assertLinked(extractor, LinkType.URL, "http://example.org/!", "|http://example.org/|!")
        assertLinked(extractor, LinkType.URL, "http://example.org/;", "|http://example.org/|;")

    }

    @Test
    fun delimiterAtEndConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://example.org/.", "|http://example.org/|.")
        assertLinked(extractor, LinkType.URL, "http://example.org/..", "|http://example.org/|..")
        assertLinked(extractor, LinkType.URL, "http://example.org/,", "|http://example.org/|,")
        assertLinked(extractor, LinkType.URL, "http://example.org/:", "|http://example.org/|:")
        assertLinked(extractor, LinkType.URL, "http://example.org/?", "|http://example.org/|?")
        assertLinked(extractor, LinkType.URL, "http://example.org/!", "|http://example.org/|!")
        assertLinked(extractor, LinkType.URL, "http://example.org/;", "|http://example.org/|;")

    }

    @Test
    fun matchingPunctuationConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://example.org/a(b)", "|http://example.org/a(b)|")
        assertLinked(extractor, LinkType.URL, "http://example.org/a[b]", "|http://example.org/a[b]|")
        assertLinked(extractor, LinkType.URL, "http://example.org/a{b}", "|http://example.org/a{b}|")
        assertLinked(extractor, LinkType.URL, "http://example.org/a'b'", "|http://example.org/a'b'|")
        assertLinked(extractor, LinkType.URL, "(http://example.org/)", "(|http://example.org/|)")
        assertLinked(extractor, LinkType.URL, "[http://example.org/]", "[|http://example.org/|]")
        assertLinked(extractor, LinkType.URL, "{http://example.org/}", "{|http://example.org/|}")
        assertLinked(extractor, LinkType.URL, "\"http://example.org/\"", "\"|http://example.org/|\"")
        assertLinked(extractor, LinkType.URL, "'http://example.org/'", "'|http://example.org/|'")
        assertLinked(extractor, LinkType.URL, "http://foo[.]example[.]org/abc/", "|http://foo[.]example[.]org/abc/|")

    }

    @Test
    fun matchingPunctuationConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://example.org/a(b)", "|http://example.org/a(b)|")
        assertLinked(extractor, LinkType.URL, "http://example.org/a[b]", "|http://example.org/a[b]|")
        assertLinked(extractor, LinkType.URL, "http://example.org/a{b}", "|http://example.org/a{b}|")
        assertLinked(extractor, LinkType.URL, "http://example.org/a'b'", "|http://example.org/a'b'|")
        assertLinked(extractor, LinkType.URL, "(http://example.org/)", "(|http://example.org/|)")
        assertLinked(extractor, LinkType.URL, "[http://example.org/]", "[|http://example.org/|]")
        assertLinked(extractor, LinkType.URL, "{http://example.org/}", "{|http://example.org/|}")
        assertLinked(extractor, LinkType.URL, "\"http://example.org/\"", "\"|http://example.org/|\"")
        assertLinked(extractor, LinkType.URL, "'http://example.org/'", "'|http://example.org/|'")
        assertLinked(extractor, LinkType.URL, "http://foo[.]example[.]org/abc/", "|http://foo[.]example[.]org/abc/|")

    }

    @Test
    fun matchingPunctuationTrickyConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "((http://example.org/))", "((|http://example.org/|))")
        assertLinked(extractor, LinkType.URL, "((http://example.org/a(b)))", "((|http://example.org/a(b)|))")
        assertLinked(extractor, LinkType.URL, "[(http://example.org/)]", "[(|http://example.org/|)]")
        assertLinked(extractor, LinkType.URL, "(http://example.org/).", "(|http://example.org/|).")
        assertLinked(extractor, LinkType.URL, "(http://example.org/.)", "(|http://example.org/|.)")
        assertLinked(extractor, LinkType.URL, "http://example.org/>", "|http://example.org/|>")
        // not sure about these
        assertLinked(extractor, LinkType.URL, "http://example.org/(", "|http://example.org/|(")
        assertLinked(extractor, LinkType.URL, "http://example.org/(.", "|http://example.org/|(.")
        assertLinked(extractor, LinkType.URL, "http://example.org/]()", "|http://example.org/|]()")

    }

    @Test
    fun matchingPunctuationTrickyConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "((http://example.org/))", "((|http://example.org/|))")
        assertLinked(extractor, LinkType.URL, "((http://example.org/a(b)))", "((|http://example.org/a(b)|))")
        assertLinked(extractor, LinkType.URL, "[(http://example.org/)]", "[(|http://example.org/|)]")
        assertLinked(extractor, LinkType.URL, "(http://example.org/).", "(|http://example.org/|).")
        assertLinked(extractor, LinkType.URL, "(http://example.org/.)", "(|http://example.org/|.)")
        assertLinked(extractor, LinkType.URL, "http://example.org/>", "|http://example.org/|>")
        // not sure about these
        assertLinked(extractor, LinkType.URL, "http://example.org/(", "|http://example.org/|(")
        assertLinked(extractor, LinkType.URL, "http://example.org/(.", "|http://example.org/|(.")
        assertLinked(extractor, LinkType.URL, "http://example.org/]()", "|http://example.org/|]()")

    }

    @Test
    fun quotesConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://example.org/'_(foo)", "|http://example.org/'_(foo)|")
        assertLinked(extractor, LinkType.URL, "http://example.org/'_(foo)'", "|http://example.org/'_(foo)'|")
        assertLinked(extractor, LinkType.URL, "http://example.org/''", "|http://example.org/''|")
        assertLinked(extractor, LinkType.URL, "http://example.org/'''", "|http://example.org/''|'")
        assertLinked(extractor, LinkType.URL, "http://example.org/'.", "|http://example.org/|'.")
        assertLinked(extractor, LinkType.URL, "http://example.org/it's", "|http://example.org/it's|")
        // " not allowed in URLs
        assertLinked(extractor, LinkType.URL, "http://example.org/\"a", "|http://example.org/|\"a")
        assertLinked(extractor, LinkType.URL, "http://example.org/\"a\"", "|http://example.org/|\"a\"")
        // ` not allowed in URLs
        assertLinked(extractor, LinkType.URL, "http://example.org/`a", "|http://example.org/|`a")
        assertLinked(extractor, LinkType.URL, "http://example.org/`a`", "|http://example.org/|`a`")

    }

    @Test
    fun quotesConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://example.org/'_(foo)", "|http://example.org/'_(foo)|")
        assertLinked(extractor, LinkType.URL, "http://example.org/'_(foo)'", "|http://example.org/'_(foo)'|")
        assertLinked(extractor, LinkType.URL, "http://example.org/''", "|http://example.org/''|")
        assertLinked(extractor, LinkType.URL, "http://example.org/'''", "|http://example.org/''|'")
        assertLinked(extractor, LinkType.URL, "http://example.org/'.", "|http://example.org/|'.")
        assertLinked(extractor, LinkType.URL, "http://example.org/it's", "|http://example.org/it's|")
        // " not allowed in URLs
        assertLinked(extractor, LinkType.URL, "http://example.org/\"a", "|http://example.org/|\"a")
        assertLinked(extractor, LinkType.URL, "http://example.org/\"a\"", "|http://example.org/|\"a\"")
        // ` not allowed in URLs
        assertLinked(extractor, LinkType.URL, "http://example.org/`a", "|http://example.org/|`a")
        assertLinked(extractor, LinkType.URL, "http://example.org/`a`", "|http://example.org/|`a`")

    }

    @Test
    fun htmlConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://example.org\">", "|http://example.org|\">")
        assertLinked(extractor, LinkType.URL, "http://example.org'>", "|http://example.org|'>")
        assertLinked(extractor, LinkType.URL, "http://example.org\"/>", "|http://example.org|\"/>")
        assertLinked(extractor, LinkType.URL, "http://example.org'/>", "|http://example.org|'/>")
        assertLinked(extractor, LinkType.URL, "http://example.org<p>", "|http://example.org|<p>")
        assertLinked(extractor, LinkType.URL, "http://example.org</p>", "|http://example.org|</p>")

    }

    @Test
    fun htmlConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://example.org\">", "|http://example.org|\">")
        assertLinked(extractor, LinkType.URL, "http://example.org'>", "|http://example.org|'>")
        assertLinked(extractor, LinkType.URL, "http://example.org\"/>", "|http://example.org|\"/>")
        assertLinked(extractor, LinkType.URL, "http://example.org'/>", "|http://example.org|'/>")
        assertLinked(extractor, LinkType.URL, "http://example.org<p>", "|http://example.org|<p>")
        assertLinked(extractor, LinkType.URL, "http://example.org</p>", "|http://example.org|</p>")

    }

    @Test
    fun cssConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://example.org\");", "|http://example.org|\");")
        assertLinked(extractor, LinkType.URL, "http://example.org');", "|http://example.org|');")

    }

    @Test
    fun cssConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://example.org\");", "|http://example.org|\");")
        assertLinked(extractor, LinkType.URL, "http://example.org');", "|http://example.org|');")

    }

    @Test
    fun slashConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://example.org/", "|http://example.org/|")
        assertLinked(extractor, LinkType.URL, "http://example.org/a/", "|http://example.org/a/|")
        assertLinked(extractor, LinkType.URL, "http://example.org//", "|http://example.org//|")

    }

    @Test
    fun slashConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://example.org/", "|http://example.org/|")
        assertLinked(extractor, LinkType.URL, "http://example.org/a/", "|http://example.org/a/|")
        assertLinked(extractor, LinkType.URL, "http://example.org//", "|http://example.org//|")

    }

    @Test
    fun multipleConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://one.org/ http://two.org/", "|http://one.org/| |http://two.org/|")
        assertLinked(extractor, LinkType.URL, "http://one.org/ : http://two.org/", "|http://one.org/| : |http://two.org/|")
        assertLinked(extractor, LinkType.URL, "(http://one.org/)(http://two.org/)", "(|http://one.org/|)(|http://two.org/|)")

    }

    @Test
    fun multipleConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://one.org/ http://two.org/", "|http://one.org/| |http://two.org/|")
        assertLinked(extractor, LinkType.URL, "http://one.org/ : http://two.org/", "|http://one.org/| : |http://two.org/|")
        assertLinked(extractor, LinkType.URL, "(http://one.org/)(http://two.org/)", "(|http://one.org/|)(|http://two.org/|)")

    }

    @Test
    fun internationalConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://üñîçøðé.com/ä", "|http://üñîçøðé.com/ä|")
        assertLinked(extractor, LinkType.URL, "http://example.org/\u00A1", "|http://example.org/\u00A1|")
        assertLinked(extractor, LinkType.URL, "http://example.org/\u00A2", "|http://example.org/\u00A2|")

    }

    @Test
    fun internationalConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, "http://üñîçøðé.com/ä", "|http://üñîçøðé.com/ä|")
        assertLinked(extractor, LinkType.URL, "http://example.org/\u00A1", "|http://example.org/\u00A1|")
        assertLinked(extractor, LinkType.URL, "http://example.org/\u00A2", "|http://example.org/\u00A2|")

    }

    @Test
    fun unicodeWhitespaceConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        val whitespace = charArrayOf(
                '\u00A0', // no-break space
                '\u2000', // en quad
                '\u2001', // em quad
                '\u2002', // en space
                '\u2003', // em space
                '\u2004', // three-per-em space
                '\u2005', // four-per-em space
                '\u2006', // six-per-em space
                '\u2007', // figure space
                '\u2008', // punctuation space
                '\u2009', // thin space
                '\u200A', // hair space
                '\u2028', // line separator
                '\u2029', // paragraph separator
                '\u202F', // narrow no-break space
                '\u205F', // medium mathematical space
                '\u3000', // ideographic space
        )

        for (c in whitespace) {
            assertLinked(extractor, LinkType.URL, "http://example.org" + c, "|http://example.org|" + c)
        }

    }

    @Test
    fun unicodeWhitespaceConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        val whitespace = charArrayOf(
                '\u00A0', // no-break space
                '\u2000', // en quad
                '\u2001', // em quad
                '\u2002', // en space
                '\u2003', // em space
                '\u2004', // three-per-em space
                '\u2005', // four-per-em space
                '\u2006', // six-per-em space
                '\u2007', // figure space
                '\u2008', // punctuation space
                '\u2009', // thin space
                '\u200A', // hair space
                '\u2028', // line separator
                '\u2029', // paragraph separator
                '\u202F', // narrow no-break space
                '\u205F', // medium mathematical space
                '\u3000', // ideographic space
        )

        for (c in whitespace) {
            assertLinked(extractor, LinkType.URL, "http://example.org" + c, "|http://example.org|" + c)
        }

    }

    @Test
    fun replyLevelConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, ">http://example.org/", ">|http://example.org/|")
        assertLinked(extractor, LinkType.URL, "> http://example.org/", "> |http://example.org/|")
        assertLinked(extractor, LinkType.URL, ">>http://example.org/", ">>|http://example.org/|")
        assertLinked(extractor, LinkType.URL, ">> http://example.org/", ">> |http://example.org/|")
        assertLinked(extractor, LinkType.URL, "> > http://example.org/", "> > |http://example.org/|")
        assertLinked(extractor, LinkType.URL, ">>>http://example.org/", ">>>|http://example.org/|")
        assertLinked(extractor, LinkType.URL, ">>> http://example.org/", ">>> |http://example.org/|")
        assertLinked(extractor, LinkType.URL, "> > > http://example.org/", "> > > |http://example.org/|")

    }

    @Test
    fun replyLevelConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        assertLinked(extractor, LinkType.URL, ">http://example.org/", ">|http://example.org/|")
        assertLinked(extractor, LinkType.URL, "> http://example.org/", "> |http://example.org/|")
        assertLinked(extractor, LinkType.URL, ">>http://example.org/", ">>|http://example.org/|")
        assertLinked(extractor, LinkType.URL, ">> http://example.org/", ">> |http://example.org/|")
        assertLinked(extractor, LinkType.URL, "> > http://example.org/", "> > |http://example.org/|")
        assertLinked(extractor, LinkType.URL, ">>>http://example.org/", ">>>|http://example.org/|")
        assertLinked(extractor, LinkType.URL, ">>> http://example.org/", ">>> |http://example.org/|")
        assertLinked(extractor, LinkType.URL, "> > > http://example.org/", "> > > |http://example.org/|")

    }

    @Test
    fun linkToStringConfiguration1() {
        val extractor = LinkExtractor.builder().linkTypes(setOf(LinkType.URL)).emailDomainMustHaveDot(true).build()
        val links = extractor.extractLinks("wow, so example: http://test.com")
        assertEquals("Link{type=URL, beginIndex=17, endIndex=32}", links.iterator().next().toString())

    }

    @Test
    fun linkToStringConfiguration2() {
        val extractor = LinkExtractor.builder().linkTypes(LinkType.entries.toSet()).emailDomainMustHaveDot(true).build()
        val links = extractor.extractLinks("wow, so example: http://test.com")
        assertEquals("Link{type=URL, beginIndex=17, endIndex=32}", links.iterator().next().toString())

    }

}
