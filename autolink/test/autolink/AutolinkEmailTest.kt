// Port of autolink-java 0.12.0 tests. Copyright (c) 2015 Robin Stocker. MIT; see UPSTREAM.md.
package io.heapy.kotmark.autolink

import kotlin.test.Test

class AutolinkEmailTest : AutolinkTestCase() {
    @Test
    fun notLinkedConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.EMAIL)
            emailDomainMustHaveDot = true
        }
        assertNotLinked(extractor, "")
        assertNotLinked(extractor, "foo")
        assertNotLinked(extractor, "@")
        assertNotLinked(extractor, "a@")
        assertNotLinked(extractor, "@a")
        assertNotLinked(extractor, "@@@")

    }

    @Test
    fun notLinkedConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        assertNotLinked(extractor, "")
        assertNotLinked(extractor, "foo")
        assertNotLinked(extractor, "@")
        assertNotLinked(extractor, "a@")
        assertNotLinked(extractor, "@a")
        assertNotLinked(extractor, "@@@")

    }

    @Test
    fun notLinkedConfiguration3() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = false
        }
        assertNotLinked(extractor, "")
        assertNotLinked(extractor, "foo")
        assertNotLinked(extractor, "@")
        assertNotLinked(extractor, "a@")
        assertNotLinked(extractor, "@a")
        assertNotLinked(extractor, "@@@")

    }

    @Test
    fun simpleConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.EMAIL)
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.EMAIL, "foo@example.com", "|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "foo.bar@example.com", "|foo.bar@example.com|")

    }

    @Test
    fun simpleConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.EMAIL, "foo@example.com", "|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "foo.bar@example.com", "|foo.bar@example.com|")

    }

    @Test
    fun simpleConfiguration3() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = false
        }
        assertLinked(extractor, LinkType.EMAIL, "foo@example.com", "|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "foo.bar@example.com", "|foo.bar@example.com|")

    }

    @Test
    fun allowedTextConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.EMAIL)
            emailDomainMustHaveDot = true
        }
        // I know, I know...
        assertLinked(extractor, LinkType.EMAIL, """#!$%&'*+-/=?^_`{}|~@example.org""", """|#!$%&'*+-/=?^_`{}|~@example.org|""")

    }

    @Test
    fun allowedTextConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        // I know, I know...
        assertLinked(extractor, LinkType.EMAIL, """#!$%&'*+-/=?^_`{}|~@example.org""", """|#!$%&'*+-/=?^_`{}|~@example.org|""")

    }

    @Test
    fun allowedTextConfiguration3() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = false
        }
        // I know, I know...
        assertLinked(extractor, LinkType.EMAIL, """#!$%&'*+-/=?^_`{}|~@example.org""", """|#!$%&'*+-/=?^_`{}|~@example.org|""")

    }

    @Test
    fun spaceSeparationConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.EMAIL)
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.EMAIL, "foo a@b.com", "foo |a@b.com|")
        assertLinked(extractor, LinkType.EMAIL, "a@b.com foo", "|a@b.com| foo")
        assertLinked(extractor, LinkType.EMAIL, "\na@b.com", "\n|a@b.com|")
        assertLinked(extractor, LinkType.EMAIL, "a@b.com\n", "|a@b.com|\n")

    }

    @Test
    fun spaceSeparationConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.EMAIL, "foo a@b.com", "foo |a@b.com|")
        assertLinked(extractor, LinkType.EMAIL, "a@b.com foo", "|a@b.com| foo")
        assertLinked(extractor, LinkType.EMAIL, "\na@b.com", "\n|a@b.com|")
        assertLinked(extractor, LinkType.EMAIL, "a@b.com\n", "|a@b.com|\n")

    }

    @Test
    fun spaceSeparationConfiguration3() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = false
        }
        assertLinked(extractor, LinkType.EMAIL, "foo a@b.com", "foo |a@b.com|")
        assertLinked(extractor, LinkType.EMAIL, "a@b.com foo", "|a@b.com| foo")
        assertLinked(extractor, LinkType.EMAIL, "\na@b.com", "\n|a@b.com|")
        assertLinked(extractor, LinkType.EMAIL, "a@b.com\n", "|a@b.com|\n")

    }

    @Test
    fun specialSeparationConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.EMAIL)
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.EMAIL, "(a@example.com)", "(|a@example.com|)")
        assertLinked(extractor, LinkType.EMAIL, """"a@example.com"""", """"|a@example.com|"""")
        assertLinked(extractor, LinkType.EMAIL, """"a@example.com"""", """"|a@example.com|"""")
        assertLinked(extractor, LinkType.EMAIL, ",a@example.com,", ",|a@example.com|,")
        assertLinked(extractor, LinkType.EMAIL, ":a@example.com:", ":|a@example.com|:")
        assertLinked(extractor, LinkType.EMAIL, ";a@example.com;", ";|a@example.com|;")

    }

    @Test
    fun specialSeparationConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.EMAIL, "(a@example.com)", "(|a@example.com|)")
        assertLinked(extractor, LinkType.EMAIL, """"a@example.com"""", """"|a@example.com|"""")
        assertLinked(extractor, LinkType.EMAIL, """"a@example.com"""", """"|a@example.com|"""")
        assertLinked(extractor, LinkType.EMAIL, ",a@example.com,", ",|a@example.com|,")
        assertLinked(extractor, LinkType.EMAIL, ":a@example.com:", ":|a@example.com|:")
        assertLinked(extractor, LinkType.EMAIL, ";a@example.com;", ";|a@example.com|;")

    }

    @Test
    fun specialSeparationConfiguration3() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = false
        }
        assertLinked(extractor, LinkType.EMAIL, "(a@example.com)", "(|a@example.com|)")
        assertLinked(extractor, LinkType.EMAIL, """"a@example.com"""", """"|a@example.com|"""")
        assertLinked(extractor, LinkType.EMAIL, """"a@example.com"""", """"|a@example.com|"""")
        assertLinked(extractor, LinkType.EMAIL, ",a@example.com,", ",|a@example.com|,")
        assertLinked(extractor, LinkType.EMAIL, ":a@example.com:", ":|a@example.com|:")
        assertLinked(extractor, LinkType.EMAIL, ";a@example.com;", ";|a@example.com|;")

    }

    @Test
    fun dotsConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.EMAIL)
            emailDomainMustHaveDot = true
        }
        assertNotLinked(extractor, ".@example.com")
        assertNotLinked(extractor, "foo.@example.com")
        assertLinked(extractor, LinkType.EMAIL, ".foo@example.com", ".|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, ".foo@example.com", ".|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "a..b@example.com", "a..|b@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "a@example.com.", "|a@example.com|.")

    }

    @Test
    fun dotsConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        assertNotLinked(extractor, ".@example.com")
        assertNotLinked(extractor, "foo.@example.com")
        assertLinked(extractor, LinkType.EMAIL, ".foo@example.com", ".|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, ".foo@example.com", ".|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "a..b@example.com", "a..|b@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "a@example.com.", "|a@example.com|.")

    }

    @Test
    fun dotsConfiguration3() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = false
        }
        assertNotLinked(extractor, ".@example.com")
        assertNotLinked(extractor, "foo.@example.com")
        assertLinked(extractor, LinkType.EMAIL, ".foo@example.com", ".|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, ".foo@example.com", ".|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "a..b@example.com", "a..|b@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "a@example.com.", "|a@example.com|.")

    }

    @Test
    fun domainWithoutDotConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.EMAIL)
            emailDomainMustHaveDot = true
        }
        if (true) {
            assertNotLinked(extractor, "a@b")
            assertNotLinked(extractor, "a@b.")
            assertLinked(extractor, LinkType.EMAIL, "a@b.com.", "|a@b.com|.")
        } else {
            assertLinked(extractor, LinkType.EMAIL, "a@b", "|a@b|")
            assertLinked(extractor, LinkType.EMAIL, "a@b.", "|a@b|.")
        }

    }

    @Test
    fun domainWithoutDotConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        if (true) {
            assertNotLinked(extractor, "a@b")
            assertNotLinked(extractor, "a@b.")
            assertLinked(extractor, LinkType.EMAIL, "a@b.com.", "|a@b.com|.")
        } else {
            assertLinked(extractor, LinkType.EMAIL, "a@b", "|a@b|")
            assertLinked(extractor, LinkType.EMAIL, "a@b.", "|a@b|.")
        }

    }

    @Test
    fun domainWithoutDotConfiguration3() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = false
        }
        if (false) {
            assertNotLinked(extractor, "a@b")
            assertNotLinked(extractor, "a@b.")
            assertLinked(extractor, LinkType.EMAIL, "a@b.com.", "|a@b.com|.")
        } else {
            assertLinked(extractor, LinkType.EMAIL, "a@b", "|a@b|")
            assertLinked(extractor, LinkType.EMAIL, "a@b.", "|a@b|.")
        }

    }

    @Test
    fun dashesConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.EMAIL)
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.EMAIL, "a@example.com-", "|a@example.com|-")
        assertLinked(extractor, LinkType.EMAIL, "a@foo-bar.com", "|a@foo-bar.com|")
        assertNotLinked(extractor, "a@-foo.com")
        if (true) {
            assertNotLinked(extractor, "a@b-.")
        } else {
            assertLinked(extractor, LinkType.EMAIL, "a@b-.", "|a@b|-.")
        }

    }

    @Test
    fun dashesConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.EMAIL, "a@example.com-", "|a@example.com|-")
        assertLinked(extractor, LinkType.EMAIL, "a@foo-bar.com", "|a@foo-bar.com|")
        assertNotLinked(extractor, "a@-foo.com")
        if (true) {
            assertNotLinked(extractor, "a@b-.")
        } else {
            assertLinked(extractor, LinkType.EMAIL, "a@b-.", "|a@b|-.")
        }

    }

    @Test
    fun dashesConfiguration3() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = false
        }
        assertLinked(extractor, LinkType.EMAIL, "a@example.com-", "|a@example.com|-")
        assertLinked(extractor, LinkType.EMAIL, "a@foo-bar.com", "|a@foo-bar.com|")
        assertNotLinked(extractor, "a@-foo.com")
        if (false) {
            assertNotLinked(extractor, "a@b-.")
        } else {
            assertLinked(extractor, LinkType.EMAIL, "a@b-.", "|a@b|-.")
        }

    }

    @Test
    fun multipleConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.EMAIL)
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.EMAIL, "a@example.com b@example.com", "|a@example.com| |b@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "a@example.com @ b@example.com", "|a@example.com| @ |b@example.com|")

    }

    @Test
    fun multipleConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.EMAIL, "a@example.com b@example.com", "|a@example.com| |b@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "a@example.com @ b@example.com", "|a@example.com| @ |b@example.com|")

    }

    @Test
    fun multipleConfiguration3() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = false
        }
        assertLinked(extractor, LinkType.EMAIL, "a@example.com b@example.com", "|a@example.com| |b@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "a@example.com @ b@example.com", "|a@example.com| @ |b@example.com|")

    }

    @Test
    fun internationalConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.EMAIL)
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.EMAIL, "üñîçøðé@example.com", "|üñîçøðé@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "üñîçøðé@üñîçøðé.com", "|üñîçøðé@üñîçøðé.com|")

    }

    @Test
    fun internationalConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.EMAIL, "üñîçøðé@example.com", "|üñîçøðé@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "üñîçøðé@üñîçøðé.com", "|üñîçøðé@üñîçøðé.com|")

    }

    @Test
    fun internationalConfiguration3() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = false
        }
        assertLinked(extractor, LinkType.EMAIL, "üñîçøðé@example.com", "|üñîçøðé@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "üñîçøðé@üñîçøðé.com", "|üñîçøðé@üñîçøðé.com|")

    }

    @Test
    fun triggerOverlapConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.EMAIL)
            emailDomainMustHaveDot = true
        }
        // 'w' is a trigger character for WWW links. Make sure we can rewind enough.
        assertLinked(extractor, LinkType.EMAIL, "www@example.com", "|www@example.com|")

    }

    @Test
    fun triggerOverlapConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        // 'w' is a trigger character for WWW links. Make sure we can rewind enough.
        assertLinked(extractor, LinkType.EMAIL, "www@example.com", "|www@example.com|")

    }

    @Test
    fun triggerOverlapConfiguration3() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = false
        }
        // 'w' is a trigger character for WWW links. Make sure we can rewind enough.
        assertLinked(extractor, LinkType.EMAIL, "www@example.com", "|www@example.com|")

    }

    @Test
    fun replyLevelConfiguration1() {
        val extractor = linkExtractor {
            linkTypes = setOf(LinkType.EMAIL)
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.EMAIL, ">foo@example.com", ">|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "> foo@example.com", "> |foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, ">>foo@example.com", ">>|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, ">> foo@example.com", ">> |foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "> > foo@example.com", "> > |foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, ">>>foo@example.com", ">>>|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, ">>> foo@example.com", ">>> |foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "> > > foo@example.com", "> > > |foo@example.com|")

    }

    @Test
    fun replyLevelConfiguration2() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = true
        }
        assertLinked(extractor, LinkType.EMAIL, ">foo@example.com", ">|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "> foo@example.com", "> |foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, ">>foo@example.com", ">>|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, ">> foo@example.com", ">> |foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "> > foo@example.com", "> > |foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, ">>>foo@example.com", ">>>|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, ">>> foo@example.com", ">>> |foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "> > > foo@example.com", "> > > |foo@example.com|")

    }

    @Test
    fun replyLevelConfiguration3() {
        val extractor = linkExtractor {
            linkTypes = LinkType.entries.toSet()
            emailDomainMustHaveDot = false
        }
        assertLinked(extractor, LinkType.EMAIL, ">foo@example.com", ">|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "> foo@example.com", "> |foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, ">>foo@example.com", ">>|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, ">> foo@example.com", ">> |foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "> > foo@example.com", "> > |foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, ">>>foo@example.com", ">>>|foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, ">>> foo@example.com", ">>> |foo@example.com|")
        assertLinked(extractor, LinkType.EMAIL, "> > > foo@example.com", "> > > |foo@example.com|")

    }
}
