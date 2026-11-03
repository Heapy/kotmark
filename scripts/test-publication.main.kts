#!/usr/bin/env kotlinr

// Test real publications from independent consumers. Run from the repository root:
// kotlinr scripts/test-publication.main.kts [--platform jvm] [--platform js]
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlin.system.exitProcess

val root = File("").canonicalFile
val fixtures = root.resolve("publication-test")
val work = root.resolve("build/publication-test")
val group = "io.heapy.kotmark.publicationtest"
val renderer = "kotmark-renderer-kotlinx-html"
val native = setOf("macosArm64", "linuxX64")
val supported = listOf("jvm", "js", "wasmJs", "wasmWasi", "linuxX64", "macosArm64")
val scenarios = linkedMapOf(
    "without-renderer" to listOf("kotmark", "kotmark-ext-math"),
    "renderer-only" to listOf("kotmark", renderer),
    "with-math" to listOf("kotmark", renderer, "kotmark-ext-math"),
    "with-highlight" to listOf("kotmark", renderer, "kotmark-ext-highlight"),
)

fun write(path: File, content: String) {
    check(path.parentFile.isDirectory || path.parentFile.mkdirs()) { "Cannot create ${path.parentFile}" }
    path.writeText(content)
}

// The reports only contain strings, booleans, lists and string-keyed maps.
// Keep the runner self-contained, without a serialization compiler plugin.
fun json(value: Any?, depth: Int = 0): String {
    val indent = "  ".repeat(depth)
    return when (value) {
        null -> "null"
        is Boolean -> value.toString()
        is String -> buildString {
            append('"')
            for (c in value) when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ' || c.isSurrogate()) append("\\u${c.code.toString(16).padStart(4, '0')}") else append(c)
            }
            append('"')
        }
        is Map<*, *> -> if (value.isEmpty()) "{}" else value.entries.joinToString(
            separator = ",\n", prefix = "{\n", postfix = "\n$indent}",
        ) { entry ->
            require(entry.key is String)
            "$indent  ${json(entry.key)}: ${json(entry.value, depth + 1)}"
        }
        is Iterable<*> -> value.joinToString(", ", "[", "]") { json(it, depth + 1) }
        else -> error("Unsupported report value: ${value::class}")
    }
}

/** Keep YAML blocks verbatim; deliberately not a general YAML parser. */
fun sections(text: String): MutableMap<String, String> {
    val starts = Regex("^([\\w@-]+):", RegexOption.MULTILINE).findAll(text).toList()
    return starts.mapIndexed { i, match ->
        match.groupValues[1] to text.substring(match.range.first, starts.getOrNull(i + 1)?.range?.first ?: text.length)
    }.toMap(linkedMapOf())
}

data class ProductionModel(val text: String, val dependencies: Set<String>, val aliases: Map<String, List<String>>)

fun productionModel(name: String, platform: String): ProductionModel {
    val blocks = sections(root.resolve("$name/module.yaml").readText())
    blocks.keys.removeAll { it.startsWith("test-") }
    val product = blocks.getValue("product")
    val targets = Regex("platforms:\\s*\\[([^]]+)\\]").find(product)
    require(targets != null && platform in targets.groupValues[1].split(',').map(String::trim)) {
        "$name does not declare $platform"
    }
    blocks["product"] = product.replaceRange(targets.range, "platforms: [$platform]")
    val aliases = Regex("^\\s+- (\\w+):\\s*\\[([^]]+)\\]", RegexOption.MULTILINE)
        .findAll(blocks.remove("aliases").orEmpty()).associate {
            it.groupValues[1] to it.groupValues[2].split(',').map(String::trim)
        }
    require("repositories" !in blocks && "plugins" !in blocks) {
        "Review publication staging for new repositories/plugins in $name"
    }
    val dependency = Regex("^\\s+- (?://|\\.\\./)([\\w-]+)(?=[:\\s]|$)", RegexOption.MULTILINE)
    val local = blocks.filterKeys { it == "dependencies" || it.startsWith("dependencies@") }
        .values.flatMap { block -> dependency.findAll(block).map { it.groupValues[1] }.toList() }.toSet()
    return ProductionModel(blocks.values.joinToString(""), local, aliases)
}

data class Staged(
    val base: File,
    val producer: File,
    val consumer: File,
    val version: String,
    val cases: Map<String, List<String>>,
    val modules: Set<String>,
)

fun stage(platform: String): Staged {
    val cases = scenarios.filterKeys { platform != "wasmWasi" || it == "without-renderer" }
    val models = linkedMapOf<String, ProductionModel>()
    val pending = cases.values.flatten().toMutableSet()
    while (pending.isNotEmpty()) {
        val name = pending.first()
        pending.remove(name)
        if (name in models) continue
        val model = productionModel(name, platform)
        models[name] = model
        pending.addAll(model.dependencies - models.keys)
    }

    // Hash sources AND publication inputs so Maven caches cannot hide scope changes.
    val inputs = mutableListOf(
        root.resolve("common.module-template.yaml"), root.resolve("library.module-template.yaml"),
        root.resolve("kotlin"), root.resolve("scripts/test-publication.main.kts"),
    )
    for (name in models.keys.sorted()) {
        val module = root.resolve(name)
        inputs.add(module.resolve("module.yaml"))
        inputs.addAll(module.walkTopDown().filter { path ->
            path.isFile && path.relativeTo(module).invariantSeparatorsPath.substringBefore('/').substringBefore('@') in setOf("src", "resources")
        })
    }
    val digest = MessageDigest.getInstance("SHA-256")
    for (path in inputs.sortedBy { it.relativeTo(root).invariantSeparatorsPath }) {
        digest.update(path.relativeTo(root).invariantSeparatorsPath.toByteArray(Charsets.UTF_8))
        digest.update(path.readBytes())
    }
    val version = "0.0.0-$platform-${digest.digest().toHexString().take(16)}"
    val base = work.resolve(platform)
    val producer = base.resolve("producer")
    val consumer = base.resolve("consumer")
    // Replace staged inputs, retaining only generated build caches.
    for (project in listOf(producer, consumer)) {
        project.listFiles()?.filter { it.name != "build" }?.forEach {
            check(it.deleteRecursively()) { "Cannot remove staged input $it" }
        }
        write(project.resolve("common.module-template.yaml"), root.resolve("common.module-template.yaml").readText())
    }
    // Preserve production settings/dependency scopes, substitute tooling plugins,
    // release coordinates and remote/signing settings for a local-only publication.
    val template = sections(root.resolve("library.module-template.yaml").readText())
    template.remove("plugins")
    require("repositories" !in template) { "Review staging before enabling release repositories in the library template" }
    var publication = template.getValue("settings")
    val overrides = mapOf("group" to group, "version" to version, "mavenCentral" to "{ enabled: false }", "signArtifacts" to "false")
    for ((key, value) in overrides) {
        val pattern = Regex("^    $key: .+$", RegexOption.MULTILINE)
        val occurrences = pattern.findAll(publication).count()
        // Templates without Central/signing configuration already use local-only defaults.
        if (occurrences == 0 && key in setOf("mavenCentral", "signArtifacts")) continue
        require(occurrences == 1) { "Expected one publishing $key in library template" }
        publication = pattern.replace(publication) { "    $key: $value" }
    }
    template["settings"] = publication
    write(producer.resolve("library.module-template.yaml"), template.values.joinToString("") +
        "repositories:\n  - url: mavenLocal\n    publish: true\n")
    write(producer.resolve("project.yaml"), "modules:\n" + models.keys.sorted().joinToString("") { "  - $it\n" })
    for ((name, model) in models) {
        write(producer.resolve("$name/module.yaml"), model.text)
        val hierarchy = mutableSetOf(platform)
        when (platform) {
            "macosArm64" -> hierarchy.addAll(listOf("native", "apple", "macos"))
            "linuxX64" -> hierarchy.addAll(listOf("native", "linux"))
            "js", "wasmJs" -> hierarchy.add("web")
        }
        for (source in root.resolve(name).listFiles().orEmpty()) {
            if (!source.isDirectory || source.name.substringBefore('@') !in setOf("src", "resources")) continue
            val kind = source.name.substringBefore('@')
            val qualifier = source.name.substringAfter('@', "")
            val destination = if (qualifier in model.aliases) {
                if (platform !in model.aliases.getValue(qualifier)) continue
                "$kind@$platform"
            } else {
                if (qualifier.isNotEmpty() && qualifier !in hierarchy) continue
                source.name
            }
            check(source.copyRecursively(producer.resolve("$name/$destination"), overwrite = true))
        }
    }
    write(consumer.resolve("project.yaml"), "modules:\n" + cases.keys.joinToString("") { "  - $it\n" })
    for ((name, selected) in cases) {
        check(fixtures.resolve(name).copyRecursively(consumer.resolve(name)))
        val dependencies = selected.joinToString("") { "  - $group:$it:$version\n" }
        write(consumer.resolve("$name/module.yaml"), """
            |apply:
            |  - //common.module-template.yaml
            |product:
            |  type: kmp/lib
            |  platforms: [$platform]
            |repositories:
            |  - url: mavenLocal
            |dependencies:
            |${dependencies.trimEnd()}
            |test-dependencies:
            |  - ${'$'}kotlin.test
            |
        """.trimMargin())
    }
    return Staged(base, producer, consumer, version, cases, models.keys.toSet())
}

/** Retain metadata and KLIB dependency manifests as diagnostic evidence. */
fun recordPublications(staged: Staged, platform: String) {
    for (module in staged.modules.sorted()) {
        for (artifact in listOf(module, "$module-${platform.lowercase()}")) {
            val published = File(System.getProperty("user.home"))
                .resolve(".m2/repository/${group.replace('.', '/')}/$artifact/${staged.version}")
            for (suffix in listOf("module", "pom")) {
                val source = published.resolve("$artifact-${staged.version}.$suffix")
                if (source.isFile) write(staged.base.resolve("metadata/$artifact.$suffix"), source.readText())
            }
            for (source in published.listFiles().orEmpty().filter { it.extension == "klib" }) {
                ZipFile(source).use { klib ->
                    for (entry in klib.entries().asSequence().filter { it.name.endsWith("/manifest") }) {
                        val text = klib.getInputStream(entry).bufferedReader().use { it.readText() }
                        write(staged.base.resolve("metadata/$artifact-manifest.txt"), text)
                    }
                }
            }
        }
    }
}

fun execute(arguments: List<String>, log: File): Boolean {
    check(log.parentFile.isDirectory || log.parentFile.mkdirs())
    val process = ProcessBuilder(arguments).directory(root).redirectErrorStream(true).redirectOutput(log).start()
    return process.waitFor() == 0
}

fun command(project: File, log: File, vararg arguments: String): Boolean {
    val passed = execute(listOf(root.resolve("kotlin").path) + arguments + listOf("--project-dir", project.path), log)
    println("${if (passed) "PASS" else "FAIL"} ${log.relativeTo(root)}")
    if (!passed) println(log.readText().takeLast(5000))
    return passed
}

fun runWeb(consumer: File, base: File, platform: String, name: String): Boolean {
    val suffix = if (platform == "js") "Js" else "WasmWasi"
    val variant = if (platform == "js") "release" else "debug"
    val task = ":$name:link${suffix}Test" + if (platform == "js") "" else "Debug"
    if (!command(consumer, base.resolve("$name-link.log"), "task", task)) return false
    val entry = consumer.resolve("build/artifacts/CompiledWebArtifact/${name}${platform}Test$variant/kotlin-output/${name}_test.mjs")
    val script = if (platform == "js") """
        const tests = [];
        globalThis.describe = (_, body) => body();
        globalThis.it = (name, body) => tests.push({name, body});
        globalThis.xdescribe = globalThis.xit = () => { throw Error('Unexpected skipped test'); };
        await import(process.argv[1]);
        if (!tests.length) throw Error('No tests discovered');
        for (const {name, body} of tests) { await body(); console.log('PASS ' + name); }
    """.trimIndent() else "const tests = await import(process.argv[1]); tests.startUnitTests();"
    val log = base.resolve("$name-run.log")
    val executed = execute(listOf("node", "--input-type=module", "-e", script, entry.toURI().toASCIIString()), log)
    val output = log.readText()
    val passed = executed && (platform != "wasmWasi" ||
        ("##teamcity[testStarted " in output && "##teamcity[testFailed " !in output))
    println("${if (passed) "PASS" else "FAIL"} $platform $name execution")
    if (!passed) println(output.takeLast(5000))
    return passed
}

fun checkGraph(graph: String, platform: String, selected: List<String>, modules: Set<String>): Map<String, Any> {
    val present = modules.filterTo(mutableSetOf()) { "$group:$it:" in graph }
    val htmlPresent = "org.jetbrains.kotlinx:kotlinx-html:" in graph
    val required = selected.toSet()
    // Accept Native's transitive extensions only for renderer consumers; record
    // them separately. This establishes neither isolation nor DCE effectiveness.
    val allowed = if (platform in native && renderer in selected) modules else required
    val unexpected = (present - allowed).sorted()
    val missing = (required - present).sorted()
    val leakedTests = "test-fixtures" in graph || "org.jsoup:jsoup:" in graph
    val passed = unexpected.isEmpty() && missing.isEmpty() && !leakedTests && htmlPresent == (renderer in selected)
    return linkedMapOf(
        "passed" to passed, "missing" to missing, "unexpected" to unexpected,
        "unselected_extensions" to (present - required).filter { it.startsWith("kotmark-ext-") }.sorted(),
        "html_present" to htmlPresent, "test_dependencies_leaked" to leakedTests,
    )
}

fun verify(platform: String): Map<String, Any> {
    val staged = stage(platform)
    println("\nTesting publication on $platform: ${staged.version}")
    val published = command(staged.producer, staged.base.resolve("publish.log"), "publish", "mavenLocal")
    val consumers = linkedMapOf<String, Map<String, Any>>()
    val result = linkedMapOf<String, Any>("version" to staged.version, "publish" to published, "consumers" to consumers)
    if (!published) {
        result["passed"] = false
        return result
    }
    recordPublications(staged, platform)
    for ((name, selected) in staged.cases) {
        val graphLog = staged.base.resolve("$name-dependencies.log")
        val resolved = command(staged.consumer, graphLog, "show", "dependencies", "-m", name, "--include-tests")
        val graph = if (resolved) checkGraph(graphLog.readText(), platform, selected, staged.modules) else mapOf("passed" to false)
        println("${if (graph["passed"] == true) "PASS" else "FAIL"} $platform $name dependency contract: ${json(graph)}")
        // Execute even after a graph failure; both checks must pass overall.
        val executed = if (platform in setOf("js", "wasmWasi")) {
            runWeb(staged.consumer, staged.base, platform, name)
        } else {
            command(staged.consumer, staged.base.resolve("$name-test.log"), "test", "-m", name, "--platform", platform)
        }
        consumers[name] = linkedMapOf("dependencies" to graph, "execution" to executed, "passed" to (graph["passed"] == true && executed))
    }
    result["passed"] = consumers.values.all { it["passed"] == true }
    return result
}

fun selectedPlatforms(): List<String> {
    val selected = mutableListOf<String>()
    var index = 0
    while (index < args.size) {
        val argument = args[index++]
        if (argument == "--help" || argument == "-h") {
            println("Usage: kotlinr scripts/test-publication.main.kts [--platform TARGET]...")
            println("Targets: ${supported.joinToString()}")
            exitProcess(0)
        }
        val platform = when {
            argument == "--platform" -> args.getOrNull(index++) ?: error("--platform requires a target")
            argument.startsWith("--platform=") -> argument.substringAfter('=')
            else -> error("Unknown argument: $argument")
        }
        require(platform in supported) { "Unknown platform '$platform'; choose from ${supported.joinToString()}" }
        selected.add(platform)
    }
    if (selected.isNotEmpty()) return selected.distinct()
    val defaults = mutableListOf("jvm", "js", "wasmJs", "wasmWasi")
    val os = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")
    if (os == "Mac OS X" && arch in setOf("aarch64", "arm64")) defaults.add("macosArm64")
    if (os == "Linux" && arch in setOf("amd64", "x86_64")) defaults.add("linuxX64")
    return defaults
}

val platforms = selectedPlatforms()
require(root.resolve("scripts/test-publication.main.kts").isFile && fixtures.isDirectory) {
    "Run from the Kotmark repository root"
}
val results = linkedMapOf<String, Map<String, Any>>()
for (platform in platforms) {
    val result = verify(platform)
    results[platform] = result
    write(work.resolve("$platform/results.json"), json(result) + "\n")
}
write(work.resolve("results.json"), json(results) + "\n")
println(json(results))
exitProcess(if (results.values.all { it["passed"] == true }) 0 else 1)
