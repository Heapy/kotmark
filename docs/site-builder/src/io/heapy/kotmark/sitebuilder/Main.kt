package io.heapy.kotmark.sitebuilder

import com.charleskorn.kaml.Yaml
import io.heapy.kotmark.docs.examples.extensions.findLinks
import io.heapy.kotmark.docs.examples.extensions.readPage
import io.heapy.kotmark.docs.examples.extensions.renderArticle
import io.heapy.kotmark.docs.examples.extensions.renderWithAlerts
import io.heapy.kotmark.docs.examples.extensions.renderWithAutolinks
import io.heapy.kotmark.docs.examples.extensions.renderWithEmoji
import io.heapy.kotmark.docs.examples.extensions.renderWithFootnotes
import io.heapy.kotmark.docs.examples.extensions.renderWithHeadingIds
import io.heapy.kotmark.docs.examples.extensions.renderWithHighlight
import io.heapy.kotmark.docs.examples.extensions.renderWithImageAttributes
import io.heapy.kotmark.docs.examples.extensions.renderWithIns
import io.heapy.kotmark.docs.examples.extensions.renderWithMath
import io.heapy.kotmark.docs.examples.extensions.renderWithStrikethrough
import io.heapy.kotmark.docs.examples.extensions.renderWithTables
import io.heapy.kotmark.docs.examples.extensions.renderWithTaskLists
import io.heapy.kotmark.docs.examples.guides.baseUrlRenderer
import io.heapy.kotmark.docs.examples.guides.renderConfigured
import io.heapy.kotmark.docs.examples.guides.routingRenderer
import io.heapy.kotmark.ext.gfm.tables.TableBody
import io.heapy.kotmark.ext.gfm.tables.TableBlock
import io.heapy.kotmark.ext.gfm.tables.TableCell
import io.heapy.kotmark.ext.gfm.tables.TablesExtension
import io.heapy.kotmark.ext.math.MathExtension
import io.heapy.kotmark.node.BulletList
import io.heapy.kotmark.node.FencedCodeBlock
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.html.HtmlNodeRendererContext
import io.heapy.kotmark.renderer.html.UrlResolver
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.html.renderChildren
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.reflect.KClass

@Serializable
private data class SiteManifest(
    val navigation: List<NavigationGroup>,
    val pages: List<String>,
)

@Serializable
private data class NavigationGroup(
    val title: String,
    val items: List<NavigationItem>,
)

@Serializable
private data class NavigationItem(
    val label: String,
    val href: String,
    val number: String? = null,
)

@Serializable
private data class PageFrontMatter(
    val title: String,
    val nav: String,
    val kicker: String,
    val description: String,
    val lede: String,
    val cards: List<SpecCard> = emptyList(),
)

@Serializable
private data class SpecCard(
    val term: String,
    val value: String,
)

private data class MarkdownPage(
    val metadata: PageFrontMatter,
    val markdown: String,
)

private data class Section(
    val id: String,
    val markdown: String,
    val headingIds: List<String>,
)

private data class CodeFence(
    val delimiter: Char,
    val length: Int,
    val suffix: String,
) {
    fun closes(fence: CodeFence): Boolean = delimiter == fence.delimiter && length >= fence.length && suffix.isBlank()
}

fun main(args: Array<String>) {
    require(args.size <= 1) { "Usage: site-builder [repository-root]" }
    val repository = Path.of(args.firstOrNull() ?: ".").toAbsolutePath().normalize()
    val output = repository.resolve("build/site")
    buildSite(repository, output)
}

internal fun buildSite(repository: Path, output: Path) {
    val pagesRoot = repository.resolve("docs/pages").normalize()
    val manifest = Yaml.default.decodeFromString<SiteManifest>(pagesRoot.resolve("site.yml").readText())
    val pagePaths = manifest.pages.map { relative ->
        val source = pagesRoot.resolve(relative).normalize()
        require(source.startsWith(pagesRoot) && source.isRegularFile()) { "Missing or unsafe page source: $relative" }
        source
    }
    Files.createDirectories(output)
    copyStaticSite(repository.resolve("docs/site"), output)
    pagePaths.forEachIndexed { index, source ->
        val markdownPage = readMarkdownPage(source.readText(), source)
        val relativeOutput = pagesRoot.relativize(source).toString().removeSuffix(".md") + ".html"
        val target = output.resolve(relativeOutput).normalize()
        Files.createDirectories(target.parent)
        val rendered = renderPage(
            repository = repository,
            manifest = manifest,
            sourcePath = relativeOutput,
            page = markdownPage,
            previous = pagePaths.getOrNull(index - 1)?.let { pageOutputPath(pagesRoot, it) },
            next = pagePaths.getOrNull(index + 1)?.let { pageOutputPath(pagesRoot, it) },
        )
        target.writeText(rendered)
    }
    println("Generated ${pagePaths.size} Markdown pages at $output")
}

private fun pageOutputPath(pagesRoot: Path, source: Path): String =
    pagesRoot.relativize(source).toString().removeSuffix(".md") + ".html"

private fun copyStaticSite(source: Path, output: Path) {
    for (name in listOf("index.html", "styles.css", "site.js", "favicon.svg")) {
        val from = source.resolve(name)
        check(from.isRegularFile()) { "Missing static site asset: $from" }
        Files.copy(from, output.resolve(name), StandardCopyOption.REPLACE_EXISTING)
    }
}

private fun readMarkdownPage(source: String, path: Path): MarkdownPage {
    require(source.startsWith("---\n")) { "$path must start with YAML front matter" }
    val closing = source.indexOf("\n---\n", startIndex = 4)
    require(closing >= 0) { "$path has no closing YAML front matter delimiter" }
    val metadata = Yaml.default.decodeFromString<PageFrontMatter>(source.substring(4, closing))
    return MarkdownPage(metadata, source.substring(closing + 5))
}

private fun renderPage(
    repository: Path,
    manifest: SiteManifest,
    sourcePath: String,
    page: MarkdownPage,
    previous: String?,
    next: String?,
): String {
    val pageSections = splitSections(page.markdown)
    val sidebar = renderSidebar(manifest.navigation, sourcePath)
    val cards = page.metadata.cards.takeIf { it.isNotEmpty() }?.let { entries ->
        "<dl class=\"spec-card\">" + entries.joinToString("") { card ->
            "<div><dt>${html(card.term)}</dt><dd>${html(card.value)}</dd></div>"
        } + "</dl>"
    }.orEmpty()
    val body = buildString {
        pageSections.intro?.let { append(renderMarkdown(repository, it, emptyList())) }
        for (section in pageSections.sections) {
            append("<section class=\"doc-section\" id=\"${html(section.id)}\" aria-labelledby=\"${html(section.id)}-title\">\n")
            append(renderMarkdown(repository, section.markdown, section.headingIds, section.id))
            append("</section>\n")
        }
    }
    val pager = buildString {
        if (previous != null || next != null) {
            append("<nav class=\"pager\" aria-label=\"Previous and next page\">")
            previous?.let { append(pagerLink(sourcePath, it, "Previous", manifest)) }
            next?.let { append(pagerLink(sourcePath, it, "Next", manifest)) }
            append("</nav>")
        }
    }
    val root = relativeHref(sourcePath, "index.html")
    val icon = relativeHref(sourcePath, "favicon.svg")
    val styles = relativeHref(sourcePath, "styles.css")
    val script = relativeHref(sourcePath, "site.js")
    val api = relativeHref(sourcePath, "api/index.html")
    val extensionIndex = relativeHref(sourcePath, "extensions/index.html")
    val config = relativeHref(sourcePath, "guides/configuration.html")
    return """<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="${html(page.metadata.description)}">
    <title>${html(page.metadata.title)} — kotmark</title>
    <link rel="icon" href="$icon" type="image/svg+xml">
    <link rel="stylesheet" href="$styles">
    <script src="$script" defer></script>
</head>
<body class="doc-body">
<a class="skip-link" href="#main">Skip to content</a>
<header class="masthead" id="top">
    <a class="wordmark" href="$root" aria-label="Kotmark home"><span class="wordmark-stamp" aria-hidden="true">K</span><span>kotmark</span></a>
    <p class="masthead-note">A Markdown engine<br>Kotlin Multiplatform</p>
    <nav class="primary-nav" aria-label="Main navigation"><a href="${relativeHref(sourcePath, "index.html#start")}">Get started</a><a href="$config">Guides</a><a href="$extensionIndex">Extensions</a><a href="$api">API</a><a href="https://github.com/Heapy/kotmark">GitHub <span aria-hidden="true">↗</span></a></nav>
</header>
<div class="doc-layout">
$sidebar
<main id="main" class="doc-main">
<header class="doc-header">
    <p class="section-kicker">${html(page.metadata.kicker)}</p>
    <h1>${html(page.metadata.title)}</h1>
    <p class="lede">${html(page.metadata.lede)}</p>
    $cards
</header>
$body
$pager
</main>
</div>
<footer class="colophon"><a class="wordmark footer-mark" href="$root"><span class="wordmark-stamp" aria-hidden="true">K</span><span>kotmark</span></a><p>Apache-2.0 · <a href="${relativeHref(sourcePath, "credits.html")}">Third-party notices</a><br><a href="https://github.com/Heapy/kotmark/issues">Report an issue</a> · <a href="${relativeHref(sourcePath, "contributing.html")}">Contributing</a></p><a href="#top">[back to top]</a></footer>
</body>
</html>
""".trimIndent()
}

private data class PageSections(
    val intro: String?,
    val sections: List<Section>,
)

private fun splitSections(markdown: String): PageSections {
    val lines = markdown.lines()
    val chunks = mutableListOf<Pair<String?, List<String>>>()
    var current = mutableListOf<String>()
    var currentTitle: String? = null
    var fence: CodeFence? = null
    for (line in lines) {
        val marker = codeFence(line)
        if (fence == null && marker != null) {
            current += line
            fence = marker
            continue
        }
        if (fence != null) {
            current += line
            if (marker?.closes(fence) == true) fence = null
            continue
        }
        if (Regex("^ {0,3}##[ \\t]+").containsMatchIn(line)) {
            if (current.isNotEmpty()) chunks += currentTitle to current
            current = mutableListOf(line)
            currentTitle = line
        } else {
            current += line
        }
    }
    if (current.isNotEmpty()) chunks += currentTitle to current
    val used = mutableSetOf<String>()
    val intro = chunks.firstOrNull()?.takeIf { it.first == null }?.second?.joinToString("\n")?.trim().orEmpty()
    val sections = chunks.filter { it.first != null }.map { pair ->
        val content = pair.second
        val text = content.joinToString("\n")
        val ids = headingIds(text, used)
        val id = ids.firstOrNull() ?: "section"
        Section(id, text, ids)
    }
    return PageSections(intro.ifEmpty { null }, sections)
}

internal fun headingIds(markdown: String, used: MutableSet<String>): List<String> {
    val result = mutableListOf<String>()
    var fence: CodeFence? = null
    for (line in markdown.lineSequence()) {
        val marker = codeFence(line)
        if (fence != null) {
            if (marker?.closes(fence) == true) fence = null
            continue
        }
        if (marker != null) {
            fence = marker
            continue
        }
        val match = Regex("^ {0,3}(#{1,6})[ \\t]+(.+?)[ \\t]*$").matchEntire(line) ?: continue
        val explicit = Regex("\\s+\\{#([A-Za-z][A-Za-z0-9_.:-]*)}$").find(match.groupValues[2])
        val rawTitle = if (explicit == null) match.groupValues[2] else match.groupValues[2].removeRange(explicit.range)
        val base = explicit?.groupValues?.get(1) ?: slug(rawTitle)
        result += uniqueId(base, used)
    }
    return result
}

private fun codeFence(line: String): CodeFence? =
    Regex("^ {0,3}(`{3,}|~{3,})(.*)$").matchEntire(line)?.let { match ->
        val delimiter = match.groupValues[1]
        CodeFence(delimiter.first(), delimiter.length, match.groupValues[2])
    }

private fun slug(text: String): String =
    text.lowercase()
        .replace(Regex("[`*_~]"), "")
        .replace(Regex("[^\\p{L}\\p{N}-]+"), "-")
        .trim('-')
        .ifEmpty { "section" }

private fun uniqueId(base: String, used: MutableSet<String>): String {
    var result = base
    var suffix = 2
    while (!used.add(result)) result = "$base-${suffix++}"
    return result
}

private fun renderMarkdown(
    repository: Path,
    markdown: String,
    headingIds: List<String>,
    sectionId: String? = null,
): String {
    val ids = headingIds.iterator()
    val sectionTitleId = sectionId?.plus("-title")
    val parser = parser { extensions(listOf(TablesExtension.create())) }
    val renderedMarkdown = markdown.replace(
        Regex("(?m)^ {0,3}(#{1,6}[ \\t]+.*?)[ \\t]+\\{#[A-Za-z][A-Za-z0-9_.:-]*}[ \\t]*$"),
        "$1",
    )
    val document = parser.parse(renderedMarkdown)
    val renderer = htmlRenderer {
        escapeHtml = true
        urlResolver = UrlResolver { _, destination -> destination.replace(Regex("\\.md(?=([?#]|$))"), ".html") }
        nodeRendererFactory { context -> siteNodeRenderer(context, repository, ids, sectionTitleId) }
        extensions(listOf(TablesExtension.create()))
    }
    return renderer.render(document)
}

private fun siteNodeRenderer(
    context: HtmlNodeRendererContext,
    repository: Path,
    ids: Iterator<String>,
    sectionTitleId: String?,
): NodeRenderer =
    object : NodeRenderer {
        override val nodeTypes: Set<KClass<out Node>> =
            setOf(Heading::class, FencedCodeBlock::class, TableBlock::class, TableCell::class, BulletList::class)

        override fun render(node: Node) {
            when (node) {
                is Heading -> {
                    val id = if (ids.hasNext()) ids.next() else "section"
                    val tag = "h${node.level}"
                    context.writer.line()
                    context.writer.tag(tag, mapOf("id" to if (id == sectionTitleId?.removeSuffix("-title")) sectionTitleId else id))
                    context.renderChildren(node)
                    context.writer.tag("/$tag")
                    context.writer.line()
                }
                is FencedCodeBlock -> renderFence(context, repository, node)
                is TableBlock -> {
                    context.writer.line()
                    context.writer.tag("div", mapOf("class" to "extension-table-wrap"))
                    context.writer.tag("table", mapOf("class" to "extension-table"))
                    context.renderChildren(node)
                    context.writer.tag("/table")
                    context.writer.tag("/div")
                    context.writer.line()
                }
                is TableCell -> {
                    val isRowHeader = node.parent?.parent is TableBody && node.parent?.firstChild === node
                    val tag = if (node.isHeader || isRowHeader) "th" else "td"
                    val attributes = linkedMapOf<String, String?>()
                    if (node.isHeader) attributes["scope"] = "col"
                    if (isRowHeader) attributes["scope"] = "row"
                    node.alignment?.let { attributes["align"] = it.name.lowercase() }
                    context.writer.line()
                    context.writer.tag(tag, context.extendAttributes(node, tag, attributes))
                    context.renderChildren(node)
                    context.writer.tag("/$tag")
                    context.writer.line()
                }
                is BulletList -> {
                    context.writer.line()
                    context.writer.tag("ul", mapOf("class" to "migration-list"))
                    context.renderChildren(node)
                    context.writer.tag("/ul")
                    context.writer.line()
                }
                else -> error("Unexpected node handled by the site renderer: ${node::class}")
            }
        }
    }

private fun renderFence(context: HtmlNodeRendererContext, repository: Path, block: FencedCodeBlock) {
    val info = block.info.orEmpty()
    val language = info.substringBefore(' ').trim().ifEmpty { "text" }
    val attributes = Regex("([A-Za-z][A-Za-z0-9_-]*)=(?:\"([^\"]*)\"|'([^']*)'|(\\S+))")
        .findAll(info)
        .associate { match ->
            val value = match.groupValues.drop(2).firstOrNull { it.isNotEmpty() }.orEmpty()
            match.groupValues[1] to value
        }
    val original = block.literal.orEmpty()
    val snippetPath = attributes["snippet"]
    val code = snippetPath?.let { readSnippet(repository, it) } ?: original
    val caption = attributes["caption"]
    val label = attributes["label"]
    val example = attributes["example"]
    val inputLabel = attributes["input"] ?: "INPUT · Markdown"
    val inputLanguage = attributes["input-language"] ?: "markdown"
    val outputLabel = attributes["output"] ?: "OUTPUT · HTML"
    val outputLanguage = attributes["output-language"] ?: "html"
    val upstream = attributes["upstream"]
    val comparison = "compare" in info.split(' ')
    context.writer.line()
    when {
        example != null -> {
            val frontMatter = if (example == "yaml-front-matter") readPage(original) else null
            val output = frontMatter?.html ?: renderExample(example, original, repository)
            context.writer.tag("div", mapOf("class" to "io-pair"))
            renderCodePanel(context, inputLabel, inputLanguage, original)
            context.writer.tag("span", mapOf("class" to "io-arrow", "aria-hidden" to "true"))
            context.writer.text("↓")
            context.writer.tag("/span")
            if (frontMatter != null) {
                renderCodePanel(context, "METADATA · Kotlin map", "text", frontMatter.metadata.toString())
            }
            renderCodePanel(context, outputLabel, outputLanguage, output)
            context.writer.tag("/div")
        }
        caption != null -> {
            context.writer.tag("div", mapOf("class" to "code-proof"))
            context.writer.tag("span", mapOf("class" to "code-caption"))
            context.writer.text(caption)
            context.writer.tag("/span")
            renderPre(context, language, code, snippetPath)
            context.writer.tag("/div")
        }
        label != null -> {
            context.writer.tag("div", mapOf("class" to "command-slip"))
            context.writer.tag("span", mapOf("class" to "source-label"))
            context.writer.text(label)
            context.writer.tag("/span")
            renderPre(context, language, code, snippetPath)
            context.writer.tag("/div")
        }
        upstream != null -> {
            context.writer.tag("details", mapOf("class" to "source-fold"))
            context.writer.tag("summary")
            context.writer.text("Upstream source: $upstream")
            context.writer.tag("/summary")
            context.writer.tag("pre", mapOf("class" to "notice"))
            context.writer.tag("code", mapOf("data-upstream" to upstream))
            context.writer.text(upstreamNotice(repository, upstream))
            context.writer.tag("/code")
            context.writer.tag("/pre")
            context.writer.tag("/details")
        }
        comparison -> {
            context.writer.tag("div", mapOf("class" to "migration-compare"))
            renderPre(context, language, code, snippetPath)
            context.writer.tag("/div")
        }
        else -> renderPre(context, language, code, snippetPath)
    }
    context.writer.line()
}

private fun renderPre(context: HtmlNodeRendererContext, language: String, code: String, snippet: String?) {
    context.writer.tag("pre")
    val attributes = mutableMapOf<String, String?>("data-language" to language)
    snippet?.let { attributes["data-snippet"] = it }
    context.writer.tag("code", attributes)
    context.writer.text(code.removeSuffix("\n"))
    context.writer.tag("/code")
    context.writer.tag("/pre")
}

private fun renderCodePanel(context: HtmlNodeRendererContext, label: String, language: String, text: String) {
    context.writer.tag("div", mapOf("class" to "io-panel"))
    context.writer.tag("span", mapOf("class" to "source-label"))
    context.writer.text(label)
    context.writer.tag("/span")
    renderPre(context, language, text, null)
    context.writer.tag("/div")
}

internal fun readSnippet(repository: Path, reference: String): String {
    val separator = reference.lastIndexOf('#')
    require(separator > 0 && separator < reference.lastIndex) { "Snippet must use file#region: $reference" }
    val sourcePath = repository.resolve(reference.substring(0, separator)).normalize()
    require(sourcePath.startsWith(repository.normalize()) && sourcePath.isRegularFile()) { "Missing or unsafe snippet source: $reference" }
    val region = reference.substring(separator + 1)
    val lines = sourcePath.readText().lines()
    val start = lines.indexOfFirst { it.trim() == "// region $region" }
    val end = lines.indexOfFirst { it.trim() == "// endregion $region" }
    require(start >= 0 && end > start) { "Missing snippet region '$region' in $sourcePath" }
    return lines.subList(start + 1, end).joinToString("\n")
}

private fun upstreamNotice(repository: Path, name: String): String {
    val source = repository.resolve("UPSTREAM.md").readText()
    val heading = Regex("(?m)^#{1,6}\\s+.*${Regex.escape(name)}.*$").find(source)
        ?: error("No upstream section for '$name' in UPSTREAM.md")
    val nextHeading = Regex("(?m)^#{1,6}\\s+").find(source, heading.range.last + 1)
    return source.substring(heading.range.last + 1, nextHeading?.range?.first ?: source.length).trim()
}

internal fun renderExample(id: String, input: String, repository: Path): String = when (id) {
    "autolink" -> renderWithAutolinks(input)
    "emoji" -> renderWithEmoji(input)
    "autolink-extractor" -> findLinks(input).joinToString("\n")
    "footnotes" -> renderWithFootnotes(input)
    "gfm-alerts" -> renderWithAlerts(input)
    "gfm-strikethrough" -> renderWithStrikethrough(input)
    "gfm-tables" -> renderWithTables(input)
    "heading-anchor" -> renderWithHeadingIds(input)
    "highlight" -> renderWithHighlight(input)
    "image-attributes" -> renderWithImageAttributes(input)
    "ins" -> renderWithIns(input)
    "math" -> renderWithMath(input)
    "task-list-items" -> renderWithTaskLists(input)
    "yaml-front-matter" -> readPage(input).html
    "configuration" -> renderConfigured(input)
    "base-url" -> baseUrlRenderer.render(parser().parse(input))
    "url-routing" -> routingRenderer.render(parser().parse(input))
    "math-renderer" -> {
        val document = io.heapy.kotmark.parser.parser { extensions(listOf(MathExtension.create())) }.parse(input)
        io.heapy.kotmark.docs.examples.extensions.mathAsCodeRenderer().render(document)
    }
    "kotlinx-html" -> renderArticle("Notes", input)
    else -> error("Unknown site example: $id")
}

private fun renderSidebar(groups: List<NavigationGroup>, sourcePath: String): String = buildString {
    append("<nav class=\"doc-index\" aria-label=\"Documentation pages\">\n")
    for (group in groups) {
        append("<p class=\"doc-index-title\">${html(group.title)}</p><ul>")
        for (item in group.items) {
            val href = relativeHref(sourcePath, item.href)
            val current = if (item.href.substringBefore('#') == sourcePath) " aria-current=\"page\"" else ""
            val number = item.number?.let { "<span class=\"doc-index-number\">${html(it)}</span>" }.orEmpty()
            append("<li><a href=\"${html(href)}\"$current>$number${html(item.label)}</a></li>")
        }
        append("</ul>\n")
    }
    append("</nav>")
}

private fun pagerLink(sourcePath: String, destination: String, label: String, manifest: SiteManifest): String {
    val target = manifest.pages.firstOrNull { pageOutputPath(Path.of("docs/pages"), Path.of("docs/pages", it)) == destination }
        ?.let { Path.of(it).fileName.toString().removeSuffix(".md") }
        ?: destination.substringAfterLast('/').removeSuffix(".html")
    val href = relativeHref(sourcePath, destination)
    return "<a class=\"pager-${label.lowercase()}\" href=\"${html(href)}\"><span>$label</span>${html(target)}</a>"
}

private fun relativeHref(sourcePage: String, target: String): String {
    val suffixAt = target.indexOfFirst { it == '#' || it == '?' }.let { if (it < 0) target.length else it }
    val path = target.substring(0, suffixAt)
    val suffix = target.substring(suffixAt)
    val from = Path.of(sourcePage).parent ?: Path.of("")
    val destination = Path.of(path)
    return from.relativize(destination).toString().replace('\\', '/') + suffix
}

private fun html(value: String): String =
    value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
