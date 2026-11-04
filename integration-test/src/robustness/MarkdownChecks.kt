package io.heapy.kotmark.robustness

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ext.autolink.AutolinkExtension
import io.heapy.kotmark.ext.footnotes.FootnotesExtension
import io.heapy.kotmark.ext.front.matter.YamlFrontMatterExtension
import io.heapy.kotmark.ext.gfm.alerts.AlertsExtension
import io.heapy.kotmark.ext.gfm.strikethrough.StrikethroughExtension
import io.heapy.kotmark.ext.gfm.tables.TablesExtension
import io.heapy.kotmark.ext.heading.anchor.HeadingAnchorExtension
import io.heapy.kotmark.ext.highlight.HighlightExtension
import io.heapy.kotmark.ext.image.attributes.ImageAttributesExtension
import io.heapy.kotmark.ext.ins.InsExtension
import io.heapy.kotmark.ext.math.MathExtension
import io.heapy.kotmark.ext.task.list.items.TaskListItemsExtension
import io.heapy.kotmark.ext.toc.tocExtension
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.Renderer
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import io.heapy.kotmark.renderer.text.textContentRenderer

/** Test infrastructure shared by common tests and the JVM fuzz target. */
fun robustnessExtensions(): List<Extension> = listOf(
    AutolinkExtension.create(),
    FootnotesExtension.create(),
    ImageAttributesExtension.create(),
    InsExtension.create(),
    HighlightExtension.create(),
    MathExtension.create(),
    AlertsExtension.create(),
    StrikethroughExtension.create(),
    TablesExtension.create(),
    TaskListItemsExtension.create(),
    YamlFrontMatterExtension.create(),
    HeadingAnchorExtension.create(),
    tocExtension(),
)

class MarkdownChecks(private val extensions: List<Extension> = emptyList()) {
    private val parsers = IncludeSourceSpans.entries.map { mode ->
        parser {
            extensions(extensions)
            includeSourceSpans = mode
        }
    }
    private val html = htmlRenderer { extensions(extensions) }
    private val markdown = markdownRenderer { extensions(extensions) }
    private val text = textContentRenderer { extensions(extensions) }

    fun checkInput(source: String) {
        val fresh = parser { extensions(extensions) }.parse(source)
        checkTree(fresh, source)
        val expected = html.render(fresh)
        for (parser in parsers) {
            // Deliberately contaminate the previous parse with reference and footnote definitions.
            val _ = parser.parse("[ref]: /previous\n\n[^note]: previous\n\n# Previous\n")
            val root = parser.parse(source)
            checkTree(root, source)
            check(html.render(root) == expected) { "Parser reuse or source span mode changed HTML" }
            for (renderer in listOf<Renderer>(html, markdown, text)) {
                val rendered = renderer.render(root)
                val appended = buildString { renderer.render(root, this) }
                check(rendered == appended) { "Renderer reuse/Appendable mismatch" }
            }
            // Exercise parsing of the Markdown renderer's output without assuming arbitrary
            // extension syntax has a lossless Markdown representation.
            val renderedSource = markdown.render(root)
            checkTree(parser.parse(renderedSource), renderedSource)
            checkTree(root, source)
        }
    }

    fun checkRoundTrip(source: String) {
        val root = parsers.first().parse(source)
        check(html.render(root) == html.render(parsers.first().parse(markdown.render(root)))) {
            "Markdown round trip changed HTML"
        }
    }

    fun checkLineEndings(source: String) {
        // Normalize first so CRLF pairs are treated as one line ending.
        val normalized = source.replace("\r\n", "\n").replace('\r', '\n')
        val expected = html.render(parsers.first().parse(normalized))
        for (ending in listOf("\r", "\r\n")) {
            check(html.render(parsers.first().parse(normalized.replace("\n", ending))) == expected) {
                "Line endings changed HTML"
            }
        }
        check(html.render(parsers.first().parse(source.replace('\u0000', '\uFFFD'))) ==
            html.render(parsers.first().parse(source))) { "NUL replacement changed HTML" }
    }
}

/** Iterative traversal catches cycles before a recursive renderer can hang or overflow. */
fun checkTree(root: Node, source: String) {
    check(root.parent == null && root.previous == null && root.next == null) { "Root is attached" }
    val pending = ArrayDeque<Node>()
    val seen = mutableSetOf<Node>()
    pending.addLast(root)
    while (pending.isNotEmpty()) {
        val node = pending.removeLast()
        check(seen.add(node)) { "Cycle or shared node in tree" }
        for (span in node.getSourceSpans()) {
            check(span.inputIndex <= source.length && span.length <= source.length - span.inputIndex) {
                "Source span outside input: $span (length ${source.length})"
            }
        }
        var previous: Node? = null
        var child = node.firstChild
        val siblings = mutableSetOf<Node>()
        while (child != null) {
            check(siblings.add(child)) { "Cycle in siblings" }
            check(child.parent === node && child.previous === previous) { "Broken parent/sibling link" }
            pending.addLast(child)
            previous = child
            child = child.next
        }
        check(previous === node.lastChild) { "Wrong last child" }
    }
}
