# kotlinx.html renderer

An optional renderer that emits **structured kotlinx.html events** from Kotmark's
AST. It supports an existing HTML DSL container, a `TagConsumer`, an `Appendable`,
or a string. Core and syntax-extension modules do not depend on it.

## Usage

In a Kotlin Toolchain project, select the renderer and the extensions you use:

```yaml
dependencies:
  - //kotmark-renderer-kotlinx-html
  - //kotmark-ext-math
```

```kotlin
import io.heapy.kotmark.ext.math.MathExtension
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.kotlinxhtml.kotlinxHtmlRenderer
import io.heapy.kotmark.renderer.kotlinxhtml.adapters.mathAdapter
import kotlinx.html.div
import kotlinx.html.span
import kotlinx.html.stream.createHTML

val parser = parser { extensions(listOf(MathExtension.create())) }
val renderer = kotlinxHtmlRenderer { adapters(mathAdapter()) }
val document = parser.parse("Energy: \$E=mc^2\$.")

val html = createHTML(prettyPrint = false).div {
    span { +"Before" }
    renderer.render(document, this)
    span { +"After" }
}
val fragment: String = renderer.render(document)
val buffer = StringBuilder()
renderer.render(document, buffer)
```

Rendering into `FlowContent` or `TagConsumer` adds no wrapper and never finalizes
the consumer. For DOM output use an open container such as `div`, so multiple
top-level blocks belong to one parent. String/Appendable output creates and
finalizes its own stream consumer. Output uses kotlinx.html serialization, such
as `<br>` and empty-valued boolean attributes, rather than byte-for-byte formatting
of the core `HtmlRenderer`.

## Adapters

Import factories from `io.heapy.kotmark.renderer.kotlinxhtml.adapters` and pass
them to `adapters(...)`. Enable the corresponding syntax extension on the parser
separately. An unhandled custom node fails explicitly instead of losing content.

| Extension | Renderer adapter |
| --- | --- |
| Math | `mathAdapter()` |
| Highlight | `highlightAdapter()` |
| Insertions | `insAdapter()` |
| GFM strikethrough | `strikethroughAdapter()` |
| GFM tables | `tablesAdapter()` |
| GFM alerts | `alertsAdapter(allowedTypes)`; standard types by default |
| Task list items | `taskListItemsAdapter()` |
| Footnotes, including inline footnotes | `footnotesAdapter()` |
| Heading anchors | `headingAnchorAdapter(defaultId, idPrefix, idSuffix)` |
| Image dimensions | `imageAttributesAdapter()` |
| YAML front matter | `yamlFrontMatterAdapter()`; emits no metadata |
| Autolinks | No adapter needed; the parser produces ordinary links |

Custom alerts need the same type/title mapping in parser and adapter. Heading
anchors need no parser extension. Footnote numbering/backlinks and duplicate
heading IDs have fresh state per render. Image attributes do not mutate the AST.
Math emits escaped TeX in `span.math.math-inline` / `div.math.math-block`; it does
not typeset formulas or load KaTeX.

## Configuration and custom handlers

Unlike the core string renderer, defaults are `escapeHtml = true` and
`sanitizeUrls = true`. Sanitization follows the core `DefaultUrlSanitizer` rules;
it is not a general HTML sanitizer. Other options are `urlResolver`, `urlSanitizer`,
`percentEncodeUrls`, `omitSingleParagraphP` and `softbreak`. URL resolution runs
before sanitization and encoding; attribute providers run afterwards.

With `escapeHtml = false`, raw HTML uses the consumer's unsafe-content API and
CommonMark block-boundary newlines are preserved. String output supports
unbalanced raw HTML; DOM consumers may require balanced fragments. Escaped mode
works with DOM output. A custom `softbreak` is trusted HTML; the default newline
is plain text.

```kotlin
import io.heapy.kotmark.node.Code
import io.heapy.kotmark.renderer.kotlinxhtml.nodeRenderer

val custom = kotlinxHtmlRenderer {
    nodeRenderer<Code> { code ->
        element(code, "code", mapOf("class" to "inline-code")) {
            text(code.literal.orEmpty())
        }
    }
}
```

`element` applies attribute providers before opening a real kotlinx.html
tag and supplies a `FlowContent` receiver for DSL calls. `text` escapes content;
`raw` inserts trusted markup. Render descendants through `render`/`renderChildren`
to preserve dispatch to other adapters. Existing `AttributeProviderFactory` and
`NodeRenderer` lifecycle hooks are supported through `nodeRendererFactory`.
Typed handlers match the exact node class and receive the context for the current render.
State captured outside the handler is shared; use a factory for per-render mutable state.

The first handler registered for a node type wins; core handlers come last.
Register overrides before adapters. Factories/providers create fresh state per
render. Configuration is snapshotted; mutable state supplied by callers in
factories, resolvers or sanitizers remains their responsibility.

## Dependencies and platforms

Core and `kotlinx.html:0.12.0` are exported dependencies. Adapter dependencies on
syntax extensions use `compile-only`: JVM/JS consumers supply the selected
extensions explicitly. Kotlin Toolchain 0.13 publishes those dependencies
transitively on Native, so Native consumers resolve all supported extensions.
There is no automatic registry retaining every adapter. DCE and final release
binary size depend on the consuming application and have not been benchmarked.

All core targets are supported **except Wasm-WASI**: kotlinx.html 0.12.0 advertises
that variant but its artifact is missing from Maven Central. This does not affect
the parser's Wasm-WASI support.

## Validation

```sh
./kotlin test -m kotmark-renderer-kotlinx-html --platform jvm
node scripts/test-js.mjs kotmark-renderer-kotlinx-html
./kotlin test -m kotmark-renderer-kotlinx-html --platform wasmJs --platform macosArm64
```

Common tests cover standard nodes, every adapter, composition, escaping, URL
processing and per-render state. JVM tests also compare HTML structure/text with
the existing renderer across both CommonMark corpora (1307 examples), exercise a
real DOM consumer, and load production classes without unselected extensions.
Independent [publication integration tests](../publication-test/README.md) check
the actual published module and dependency isolation from separate consumers:

```sh
kotlinr scripts/test-publication.main.kts --platform jvm
```
