#!/usr/bin/env kotlinr

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.writeText
import org.w3c.dom.Element

require(args.size <= 1) { "Usage: kotlinr docs/assemble.main.kts [BUILD_DIRECTORY]" }
val repository = Path.of("").toAbsolutePath().normalize()
val build = Path.of(args.firstOrNull() ?: "build").let { if (it.isAbsolute) it else repository.resolve(it) }.normalize()
val siteSource = repository.resolve("build/site")
val site = build.resolve("pages")
require(siteSource.resolve("index.html").isRegularFile()) { "Generated site is missing from $siteSource; run ./kotlin run --platform jvm -m site-builder first" }

fun copyTree(source: Path, target: Path) {
    require(source.isDirectory()) { "Report directory does not exist: $source" }
    Files.walk(source).use { paths ->
        paths.forEach { path ->
            val destination = target.resolve(source.relativize(path))
            if (Files.isDirectory(path)) {
                Files.createDirectories(destination)
            } else {
                Files.createDirectories(destination.parent)
                Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}

fun html(value: String): String =
    value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

fun taskDirectories(root: Path, suffix: String): List<Path> {
    require(root.isDirectory()) { "Report task directory does not exist: $root" }
    return Files.list(root).use { paths ->
        paths.filter { path ->
            Files.isDirectory(path) && path.fileName.toString().startsWith("_") && path.fileName.toString().endsWith(suffix)
        }.toList()
    }
}

fun reportPage(title: String, kicker: String, lede: String, table: String): String = """<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>$title — kotmark</title>
    <link rel="icon" href="../favicon.svg" type="image/svg+xml">
    <link rel="stylesheet" href="../styles.css">
</head>
<body class="doc-body">
<a class="skip-link" href="#main">Skip to content</a>
<header class="masthead" id="top">
    <a class="wordmark" href="../index.html" aria-label="Kotmark home"><span class="wordmark-stamp" aria-hidden="true">K</span><span>kotmark</span></a>
    <p class="masthead-note">A Markdown engine<br>Kotlin Multiplatform</p>
    <nav class="primary-nav" aria-label="Main navigation"><a href="../index.html#start">Get started</a><a href="../guides/configuration.html">Guides</a><a href="../extensions/index.html">Extensions</a><a href="../api/index.html">API</a><a href="https://github.com/Heapy/kotmark">GitHub <span aria-hidden="true">↗</span></a></nav>
</header>
<main id="main" class="doc-main">
<header class="doc-header"><p class="section-kicker">$kicker</p><h1>$title</h1><p class="lede">$lede</p></header>
<section class="doc-section" id="modules" aria-labelledby="modules-title"><h2 id="modules-title">Modules</h2>
$table
</section>
</main>
<footer class="colophon"><a class="wordmark footer-mark" href="../index.html"><span class="wordmark-stamp" aria-hidden="true">K</span><span>kotmark</span></a><p>Apache-2.0 · <a href="../credits.html">Third-party notices</a><br><a href="https://github.com/Heapy/kotmark/issues">Report an issue</a> · <a href="../contributing.html">Contributing</a></p><a href="#top">[back to top]</a></footer>
</body>
</html>
"""

copyTree(siteSource, site)

val dokkaTasks = taskDirectories(build.resolve("documentation/tasks"), "_dokkaHtml@dokka")
check(dokkaTasks.isNotEmpty()) { "No Dokka HTML task reports found under ${build.resolve("documentation/tasks")}" }
val apiEntries = dokkaTasks.map { task ->
    val module = task.fileName.toString().removePrefix("_").removeSuffix("_dokkaHtml@dokka")
    val report = task.resolve("html")
    check(report.resolve("index.html").isRegularFile()) { "Dokka index is missing: $report/index.html" }
    copyTree(report, site.resolve("api/$module"))
    module
}.distinct().sorted()
check(apiEntries.size == dokkaTasks.size) { "Duplicate Dokka module reports found" }
val apiRows = apiEntries.joinToString("\n") { module ->
    "<tr><th scope=\"row\"><a href=\"${html(module)}/index.html\">${html(module)}</a></th><td>Kotlin Multiplatform API reference</td></tr>"
}
site.resolve("api/index.html").apply {
    parent?.let { Files.createDirectories(it) }
    writeText(
        reportPage(
            title = "API reference",
            kicker = "Generated reference / Dokka",
            lede = "Public and protected API documentation, generated for this revision.",
            table = "<div class=\"extension-table-wrap\"><table class=\"extension-table\"><thead><tr><th scope=\"col\">Module</th><th scope=\"col\">Reference</th></tr></thead><tbody>$apiRows</tbody></table></div>",
        ),
    )
}

val coverageTasks = taskDirectories(build.resolve("tasks"), "_koverReport@kover")
check(coverageTasks.isNotEmpty()) { "No Kover HTML task reports found under ${build.resolve("tasks")}" }
val xmlFactory = DocumentBuilderFactory.newInstance().apply {
    setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
    setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
    isXIncludeAware = false
    isExpandEntityReferences = false
}
data class CoverageEntry(val module: String, val missed: Int, val covered: Int)
val coverageEntries = coverageTasks.map { task ->
    val module = task.fileName.toString().removePrefix("_").removeSuffix("_koverReport@kover")
    val report = task.resolve("html")
    val xml = task.resolve("coverage.xml")
    check(report.resolve("index.html").isRegularFile()) { "Kover HTML index is missing: $report/index.html" }
    check(xml.isRegularFile()) { "Kover XML report is missing: $xml" }
    copyTree(report, site.resolve("coverage/$module"))
    val document = xmlFactory.newDocumentBuilder().parse(xml.toFile())
    val root = document.documentElement
    val lineCounter = (0 until root.childNodes.length)
        .mapNotNull { root.childNodes.item(it) as? Element }
        .firstOrNull { it.tagName == "counter" && it.getAttribute("type") == "LINE" }
        ?: error("No report-level LINE counter found in $xml")
    CoverageEntry(module, lineCounter.getAttribute("missed").toInt(), lineCounter.getAttribute("covered").toInt())
}.distinctBy { it.module }.sortedBy { it.module }
check(coverageEntries.size == coverageTasks.size) { "Duplicate Kover module reports found" }
val coverageRows = coverageEntries.joinToString("\n") { entry ->
    val total = entry.missed + entry.covered
    val percentage = if (total == 0) "n/a" else String.format(Locale.ROOT, "%.1f%%", entry.covered * 100.0 / total)
    "<tr><th scope=\"row\"><a href=\"${html(entry.module)}/index.html\">${html(entry.module)}</a></th><td>$percentage</td><td>${entry.covered} / $total lines</td></tr>"
}
site.resolve("coverage/index.html").apply {
    parent?.let { Files.createDirectories(it) }
    writeText(
        reportPage(
            title = "Coverage",
            kicker = "Generated report / Kover · JVM line coverage",
            lede = "Coverage is reported per production module. These figures describe executed JVM lines only.",
            table = "<div class=\"extension-table-wrap\"><table class=\"extension-table\"><thead><tr><th scope=\"col\">Module</th><th scope=\"col\">Line coverage</th><th scope=\"col\">Covered</th></tr></thead><tbody>$coverageRows</tbody></table></div>",
        ),
    )
}

println("Assembled ${apiEntries.size} API references and ${coverageEntries.size} JVM coverage reports at $site")