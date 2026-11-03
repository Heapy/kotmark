package io.heapy.kotmark.sitebuilder

import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class SiteBuilderTest {
    @Test
    fun `manifest pages render with source-backed code and links`() {
        val repository = repositoryRoot()
        val output = repository.resolve("build/site-builder-test")
        buildSite(repository, output)

        val autolink = output.resolve("extensions/autolink.html").readText()
        assertContains(autolink, "data-snippet=\"docs/examples/src/io/heapy/kotmark/docs/examples/extensions/Autolink.kt#autolink\"")
        assertContains(autolink, "class=\"io-pair\"")
        assertContains(autolink, "gfm-differences")
        assertFalse(autolink.contains("{#gfm-differences}"))

        val extensions = output.resolve("extensions/index.html").readText()
        assertContains(extensions, "href=\"autolink.html\"")
        assertFalse(Regex("href=\"[^\"]+\\.md(?:[?#\"]|$)").containsMatchIn(extensions))

        assertContains(extensions, "class=\"extension-table-wrap\"")
        assertContains(extensions, "scope=\"col\"")
        assertContains(extensions, "scope=\"row\"")

        val credits = output.resolve("credits.html").readText()
        assertContains(credits, "data-upstream=\"MIT notice for autolink\"")
        assertContains(credits, "Copyright (c) 2015 Robin Stocker")
    }

    @Test
    fun `unknown examples and source regions fail loudly`() {
        val repository = repositoryRoot()

        assertFailsWith<IllegalStateException> {
            renderExample("not-registered", "", repository)
        }
        assertFailsWith<IllegalArgumentException> {
            readSnippet(repository, "docs/examples/src/io/heapy/kotmark/docs/examples/extensions/Autolink.kt#missing")
        }
    }

    @Test
    fun `headings inside code fences do not affect generated IDs`() {
        val ids = headingIds(
            """
            ## Example
            ````markdown
            ## This is code
            ```
            ## Still code
            ````
            ## Actual section
            """.trimIndent(),
            mutableSetOf(),
        )

        assertEquals(listOf("example", "actual-section"), ids)
    }

    private fun repositoryRoot(): Path {
        var directory = Path.of("").toAbsolutePath().normalize()
        while (!directory.resolve("docs/pages/site.yml").isRegularFile()) {
            directory = requireNotNull(directory.parent) { "Could not locate docs/pages/site.yml from ${Path.of("").toAbsolutePath()}" }
        }
        return directory
    }
}