// Port of autolink-java 0.12.0 tests. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
package io.heapy.kotmark.autolink

import kotlin.test.Test

class AutolinkWwwTest : AutolinkTestCase() {
    @Test
    fun notLinkedConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.WWW)
            emailDomainMustHaveDot = true
        }
        assertNotLinked(extractor, "")
        assertNotLinked(extractor, "wwwsomething.com")
        assertNotLinked(extractor, "ww.foo.com")
        assertNotLinked(extractor, "w.bar.foo.co")
        assertNotLinked(extractor, "www.something")
        assertNotLinked(extractor, "www.go")
        assertNotLinked(extractor, "foo.www.fo.uk")
        assertNotLinked(extractor, "www..com")
        assertNotLinked(extractor, "wwww.toomany.com")

    }

    @Test
    fun notLinkedConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        assertNotLinked(extractor, "")
        assertNotLinked(extractor, "wwwsomething.com")
        assertNotLinked(extractor, "ww.foo.com")
        assertNotLinked(extractor, "w.bar.foo.co")
        assertNotLinked(extractor, "www.something")
        assertNotLinked(extractor, "www.go")
        assertNotLinked(extractor, "foo.www.fo.uk")
        assertNotLinked(extractor, "www..com")
        assertNotLinked(extractor, "wwww.toomany.com")

    }

    @Test
    fun linkedConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.WWW)
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.WWW, "www.s.com", "|www.s.com|")
        assertLinked(extractor, LinkType.WWW, "www.fo.uk", "|www.fo.uk|")
        assertLinked(extractor, LinkType.WWW, "foo:www.fo.uk", "foo:|www.fo.uk|")
        assertLinked(extractor, LinkType.WWW, "foo-www.fo.uk", "foo-|www.fo.uk|")

    }

    @Test
    fun linkedConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.WWW, "www.s.com", "|www.s.com|")
        assertLinked(extractor, LinkType.WWW, "www.fo.uk", "|www.fo.uk|")
        assertLinked(extractor, LinkType.WWW, "foo:www.fo.uk", "foo:|www.fo.uk|")
        assertLinked(extractor, LinkType.WWW, "foo-www.fo.uk", "foo-|www.fo.uk|")

    }

    @Test
    fun htmlConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.WWW)
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.WWW, """<a href="somelink">www.example.org</a>""", """<a href="somelink">|www.example.org|</a>""")
        assertLinked(extractor, LinkType.WWW, """<a href="www.example.org">sometext</a>""", """<a href="|www.example.org|">sometext</a>""")
        assertLinked(extractor, LinkType.WWW, "<p>www.example.org</p>", "<p>|www.example.org|</p>")

    }

    @Test
    fun htmlConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.WWW, """<a href="somelink">www.example.org</a>""", """<a href="somelink">|www.example.org|</a>""")
        assertLinked(extractor, LinkType.WWW, """<a href="www.example.org">sometext</a>""", """<a href="|www.example.org|">sometext</a>""")
        assertLinked(extractor, LinkType.WWW, "<p>www.example.org</p>", "<p>|www.example.org|</p>")

    }

    @Test
    fun multipleConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.WWW)
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.WWW, "www.one.org/ www.two.org/", "|www.one.org/| |www.two.org/|")
        assertLinked(extractor, LinkType.WWW, "www.one.org/ : www.two.org/", "|www.one.org/| : |www.two.org/|")
        assertLinked(extractor, LinkType.WWW, "(www.one.org/)(www.two.org/)", "(|www.one.org/|)(|www.two.org/|)")

    }

    @Test
    fun multipleConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.WWW, "www.one.org/ www.two.org/", "|www.one.org/| |www.two.org/|")
        assertLinked(extractor, LinkType.WWW, "www.one.org/ : www.two.org/", "|www.one.org/| : |www.two.org/|")
        assertLinked(extractor, LinkType.WWW, "(www.one.org/)(www.two.org/)", "(|www.one.org/|)(|www.two.org/|)")

    }

    @Test
    fun internationalConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.WWW)
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.WWW, "www.üñîçøðé.com/ä", "|www.üñîçøðé.com/ä|")
        assertLinked(extractor, LinkType.WWW, "www.example.org/\u00A1", "|www.example.org/\u00A1|")
        assertLinked(extractor, LinkType.WWW, "www.example.org/\u00A2", "|www.example.org/\u00A2|")

    }

    @Test
    fun internationalConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.WWW, "www.üñîçøðé.com/ä", "|www.üñîçøðé.com/ä|")
        assertLinked(extractor, LinkType.WWW, "www.example.org/\u00A1", "|www.example.org/\u00A1|")
        assertLinked(extractor, LinkType.WWW, "www.example.org/\u00A2", "|www.example.org/\u00A2|")

    }

    @Test
    fun replyLevelConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.WWW)
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.WWW, ">www.example.org/", ">|www.example.org/|")
        assertLinked(extractor, LinkType.WWW, "> www.example.org/", "> |www.example.org/|")
        assertLinked(extractor, LinkType.WWW, ">>www.example.org/", ">>|www.example.org/|")
        assertLinked(extractor, LinkType.WWW, ">> www.example.org/", ">> |www.example.org/|")
        assertLinked(extractor, LinkType.WWW, "> > www.example.org/", "> > |www.example.org/|")
        assertLinked(extractor, LinkType.WWW, ">>>www.example.org/", ">>>|www.example.org/|")
        assertLinked(extractor, LinkType.WWW, ">>> www.example.org/", ">>> |www.example.org/|")
        assertLinked(extractor, LinkType.WWW, "> > > www.example.org/", "> > > |www.example.org/|")

    }

    @Test
    fun replyLevelConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.WWW, ">www.example.org/", ">|www.example.org/|")
        assertLinked(extractor, LinkType.WWW, "> www.example.org/", "> |www.example.org/|")
        assertLinked(extractor, LinkType.WWW, ">>www.example.org/", ">>|www.example.org/|")
        assertLinked(extractor, LinkType.WWW, ">> www.example.org/", ">> |www.example.org/|")
        assertLinked(extractor, LinkType.WWW, "> > www.example.org/", "> > |www.example.org/|")
        assertLinked(extractor, LinkType.WWW, ">>>www.example.org/", ">>>|www.example.org/|")
        assertLinked(extractor, LinkType.WWW, ">>> www.example.org/", ">>> |www.example.org/|")
        assertLinked(extractor, LinkType.WWW, "> > > www.example.org/", "> > > |www.example.org/|")

    }
}
