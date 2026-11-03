package io.heapy.kotmark.renderer.kotlinxhtml

import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.testfixtures.example.ExampleReader
import kotlinx.html.div
import kotlinx.html.dom.createHTMLTree
import kotlinx.html.span
import org.jsoup.Jsoup
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.*

class HtmlCompatibilityTest {
    @Test
    fun commonmarkCorporaHaveTheSameHtmlStructureAndTextAsTheCoreRenderer() {
        val parser = parser()
        val reference = htmlRenderer { percentEncodeUrls = true }
        val renderer = kotlinxHtmlRenderer { escapeHtml = false; sanitizeUrls = false; percentEncodeUrls = true }
        val failures = mutableListOf<String>()
        val examples = listOf("commonmark-java/spec.txt", "commonmark-spec/spec.txt")
            .flatMap(ExampleReader::readExamples)
        assertEquals(1307, examples.size)
        for (example in examples) {
            val document = parser.parse(example.source)
            val expected = canonical(reference.render(document))
            val actual = canonical(renderer.render(document))
            if (expected != actual) failures += "$example\n${example.source}\nexpected: $expected\nactual: $actual"
        }
        assertTrue(failures.isEmpty(), "${failures.size} mismatches:\n" + failures.take(12).joinToString("\n\n"))
    }

    // Ignore serialization differences such as <br> vs <br /> and whitespace
    // between block tags, while comparing every element, attribute and visible
    // text. Code/pre content is compared verbatim.
    private fun canonical(html: String): List<String> = Jsoup.parseBodyFragment(html).body().allElements.map { element ->
        val attrs = element.attributes().asList().sortedBy { it.key }.map { it.key to it.value }
        val text = if (element.tagName() in setOf("pre", "code")) element.wholeText() else element.text()
        "${element.tagName()} $attrs text=$text data=${element.data()}"
    }

    @Test
    fun rendersRealDomNodesAndLeavesTheParentOpen() {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val element = document.createHTMLTree().div {
            kotlinxHtmlRenderer().render(parser().parse("# Heading\n\nA **bold** & <b>tag</b>\n\n- item"), this)
            span { +"after" }
        }
        assertEquals("h1", element.firstChild.nodeName)
        assertEquals("Heading", element.firstChild.textContent)
        assertEquals(1, element.getElementsByTagName("strong").length)
        assertEquals(0, element.getElementsByTagName("b").length)
        assertEquals("A bold & <b>tag</b>", element.getElementsByTagName("p").item(0).textContent)
        assertEquals("after", element.lastChild.textContent)
    }
}
