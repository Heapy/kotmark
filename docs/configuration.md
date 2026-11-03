# Configuration

Create parsers and renderers with configuration blocks. Omitting the block uses the defaults.

```kotlin
import io.heapy.kotmark.ext.gfm.strikethrough.strikethrough
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.IncludeSourceSpans
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

val extensions = extensions {
    strikethrough { requireTwoTildes = true }
}
val parser = parser {
    includeSourceSpans = IncludeSourceSpans.BLOCKS_AND_INLINES
    extensions(extensions)
}
val renderer = htmlRenderer {
    escapeHtml = true
    softbreak = "<br />\n"
    extensions(extensions)
}
val html = renderer.render(parser.parse("Hello **Kotlin** and ~~builders~~!"))
```

## Extension lists

`extensions { … }` builds a read-only `List<Extension>` snapshot to share between
parsers and renderers. Import its registration functions from the optional modules,
for example `io.heapy.kotmark.ext.gfm.tables.tables` for `tables { maxCells = 10_000 }`.
Use `add(customExtension)` for an existing instance or `addAll(existingExtensions)`
to append another collection. Order and duplicates are preserved; custom renderer
overrides must precede the extensions that supply defaults.

Later changes to the builder or source collections do not change the list.
Extension objects and callbacks remain shared. Parsers and renderers apply only
the extension interfaces they support. The kotlinx.html renderer uses adapters
for extension output instead of this list.

## Factories

Each factory is in the same package as its class:

| Class | Factory |
| --- | --- |
| `Parser` | `parser { … }` |
| `HtmlRenderer` | `htmlRenderer { … }` |
| `MarkdownRenderer` | `markdownRenderer { … }` |
| `TextContentRenderer` | `textContentRenderer { … }` |
| `AutolinkExtension` | `autolinkExtension { … }` |
| `FootnotesExtension` | `footnotesExtension { … }` |
| `AlertsExtension` | `alertsExtension { … }` |
| `StrikethroughExtension` | `strikethroughExtension { … }` |
| `TablesExtension` | `tablesExtension { … }` |
| `HeadingAnchorExtension` | `headingAnchorExtension { … }` |
| `IdGenerator` | `idGenerator { … }` |
| `LinkExtractor` | `linkExtractor { … }` |
| `AsciiMatcher` | `asciiMatcher { … }`, `asciiMatcher(existing) { … }` |

Scalar options are properties. Registration functions such as `extensions(...)`,
`nodeRenderer<N>(...)`, `nodeRendererFactory(...)`, and `postProcessor(...)` return `Unit` and run in order.
Register extensions with the parser and each renderer that should handle them.

Each factory uses a fresh configuration and snapshots its options and registrations.
Later changes to a captured configuration do not affect the created instance.
Registered factories, processors, sanitizers, and URL resolvers themselves are shared, so their
own mutable state remains the caller's responsibility. DSL markers prevent nested
blocks in the same DSL family from implicitly configuring an outer receiver.

See [migration](migration.md) for changes from builder-based APIs.

## Typed node renderers

Register a handler with the concrete node type. Its receiver is the context for
the current render, so no anonymous `NodeRenderer`, `nodeTypes`, or cast is needed:

```kotlin
import io.heapy.kotmark.node.Heading
import io.heapy.kotmark.renderer.html.htmlRenderer
import io.heapy.kotmark.renderer.html.nodeRenderer
import io.heapy.kotmark.renderer.html.renderChildren

val renderer = htmlRenderer {
    nodeRenderer<Heading> { heading ->
        val tag = "h${heading.level}"
        writer.tag(tag, extendAttributes(heading, tag, mapOf("class" to "chapter-title")))
        renderChildren(heading)
        writer.tag("/$tag")
    }
}
```

The HTML, Markdown, text-content, and kotlinx.html renderer packages each expose
`nodeRenderer<N>`. Import it from the package of the renderer you configure.
Matching uses the exact runtime class; registering a base class does not register
its subclasses. Register multiple typed handlers for multiple node types.
The first registration wins, including when mixing typed handlers and factories;
register overrides before extensions or adapters. Built-in handlers come last.

Markdown handlers can declare characters that ordinary text must escape:

```kotlin
import io.heapy.kotmark.node.Code
import io.heapy.kotmark.renderer.markdown.markdownRenderer
import io.heapy.kotmark.renderer.markdown.nodeRenderer

val markdown = markdownRenderer {
    nodeRenderer<Code>(specialCharacters = setOf('=')) { code ->
        writer.raw("==")
        writer.raw(code.literal.orEmpty())
        writer.raw("==")
    }
}
```

The character set is copied at registration. Core contexts also provide a
`renderChildren` extension in their respective packages; kotlinx.html has it as
a context member. It delegates each child to its registered handler and saves
the next sibling before rendering, preserving traversal when a handler unlinks
the current node. Traversal remains synchronous and recursive.

Typed handlers have a fresh context on every render, but mutable variables
captured outside their bodies are shared. Use `nodeRendererFactory` for state
created per render and for `beforeRoot`/`afterRoot` hooks. Inside a factory,
`io.heapy.kotmark.renderer.typedNodeRenderer<N> { node -> ... }` creates a typed
`NodeRenderer` without hooks. The existing interfaces remain available.

Custom handlers own their output: use `extendAttributes` to apply attribute
providers, and resolve, sanitize, and encode custom link/image destinations as
described in [URL resolution](url-resolution.md).
