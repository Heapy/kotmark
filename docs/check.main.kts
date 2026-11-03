#!/usr/bin/env kotlinr
@file:DependsOn("org.jsoup:jsoup:1.23.2")

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.readLines
import kotlin.io.path.readText
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

require(args.isEmpty() || (args.size == 2 && args[0] == "--assembled")) {
    "Usage: kotlinr docs/check.main.kts [--assembled SITE_DIRECTORY]"
}
val repository = Path.of("").toAbsolutePath().normalize()
val assembled = args.isNotEmpty()
val generated = repository.resolve("build/site")
val site = if (assembled) Path.of(args[1]).toAbsolutePath().normalize() else generated
require(site.resolve("index.html").isRegularFile()) { "Site page does not exist: ${site.resolve("index.html")}" }

// The generated site pages, plus the report indexes when checking an assembled site.
val pagePaths = Files.walk(generated).use { paths ->
    paths.filter { path ->
        val relative = generated.relativize(path).toString().replace('\\', '/')
        path.isRegularFile() && path.extension == "html" && !relative.startsWith("api/") && !relative.startsWith("coverage/")
    }.map { generated.relativize(it).toString() }.toList()
}.sorted() + if (assembled) listOf("api/index.html", "coverage/index.html") else emptyList()

val documents = mutableMapOf<Path, Document>()
fun document(path: Path): Document = documents.getOrPut(path) { Jsoup.parse(path.readText(), path.toUri().toString()) }
fun isGenerated(relative: String) = relative.startsWith("api/") || relative.startsWith("coverage/")

val upstream = repository.resolve("UPSTREAM.md").readLines()
fun upstreamSection(title: String): String {
    val start = upstream.indexOf("## $title")
    check(start >= 0) { "UPSTREAM.md has no section '$title'" }
    val end = (start + 1 until upstream.size).firstOrNull { upstream[it].startsWith("## ") } ?: upstream.size
    return upstream.subList(start + 1, end).joinToString("\n").trim('\n')
}

var checkedLinks = 0
var checkedSnippets = 0
var checkedNotices = 0
var checkedAnchors = 0
for (relativePage in pagePaths) {
    val pagePath = site.resolve(relativePage).normalize()
    check(pagePath.isRegularFile()) { "Page does not exist: $pagePath" }
    val page = document(pagePath)
    val ids = mutableSetOf<String>()
    for (element in page.select("[id]")) {
        check(ids.add(element.id())) { "$relativePage: duplicate ID ${element.id()}" }
    }
    checkedAnchors += ids.size
    for (image in page.select("img")) {
        check(image.attr("alt").isNotBlank()) { "$relativePage: an image is missing its description" }
    }

    for (element in page.allElements) {
        for (attribute in listOf("src", "href")) {
            if (!element.hasAttr(attribute)) continue
            val link = element.attr(attribute)
            val uri = URI(link)
            check(!uri.path.orEmpty().endsWith(".md")) { "$relativePage: link to a Markdown file instead of a site page: $link" }
            if (uri.isAbsolute || uri.rawAuthority != null) continue
            val path = uri.path.orEmpty()
            if (path.isEmpty()) {
                if (!uri.fragment.isNullOrEmpty()) check(uri.fragment in ids) { "$relativePage: broken anchor $link" }
                checkedLinks++
                continue
            }
            check(!path.startsWith("/")) { "$relativePage: use a relative URL: $link" }
            val target = pagePath.parent.resolve(path).normalize()
            check(target.startsWith(site)) { "$relativePage: link escapes the site directory: $link" }
            val targetRelative = site.relativize(target).toString()
            if (!target.isRegularFile() && isGenerated(targetRelative) && !assembled) continue
            check(target.isRegularFile()) { "$relativePage: broken local link $link" }
            if (!uri.fragment.isNullOrEmpty() && target.extension == "html" && !isGenerated(targetRelative)) {
                check(document(target).getElementById(uri.fragment) != null) { "$relativePage: broken anchor $link" }
            }
            checkedLinks++
        }
    }

    for (code in page.select("code[data-snippet]")) {
        val (relativePath, region) = code.attr("data-snippet").split("#", limit = 2).let { parts ->
            require(parts.size == 2) { "$relativePage: snippet must use file#region: ${code.attr("data-snippet")}" }
            parts[0] to parts[1]
        }
        val snippetSource = repository.resolve(relativePath).normalize()
        check(snippetSource.startsWith(repository) && snippetSource.isRegularFile()) { "$relativePage: snippet source does not exist: $relativePath" }
        val lines = snippetSource.readLines()
        val start = lines.indexOf("// region $region")
        check(start >= 0) { "$relativePage: missing source region '$region' in $relativePath" }
        val end = (start + 1 until lines.size).firstOrNull { lines[it] == "// endregion $region" }
        check(end != null) { "$relativePage: missing end marker for '$region' in $relativePath" }
        val expected = lines.subList(start + 1, end).joinToString("\n")
        check(code.wholeText() == expected) { "$relativePage: snippet differs from $relativePath#$region" }
        checkedSnippets++
    }

    for (code in page.select("code[data-upstream]")) {
        val title = code.attr("data-upstream")
        check(code.wholeText() == upstreamSection(title)) { "$relativePage: notice differs from UPSTREAM.md section '$title'" }
        checkedNotices++
    }
}

println(
    "Checked ${pagePaths.size} pages: $checkedLinks local links, $checkedSnippets source snippets, " +
        "$checkedNotices license notices, and $checkedAnchors unique anchors.",
)
