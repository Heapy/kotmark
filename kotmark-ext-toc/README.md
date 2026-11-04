# Table of contents

`io.heapy.kotmark:kotmark-ext-toc:0.1.0` is an opt-in Kotlin Multiplatform extension.
Register it with both the parser and the renderers you use:

```kotlin
import io.heapy.kotmark.ext.toc.toc
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

val extensions = extensions { toc() }
val document = parser { extensions(extensions) }.parse("[TOC]\n\n# Guide\n\n## Install")
val html = htmlRenderer { extensions(extensions) }.render(document)
```

Output:

```html
<ul>
<li><a href="#guide">Guide</a>
<ul>
<li><a href="#install">Install</a></li>
</ul>
</li>
</ul>
<h1 id="guide">Guide</h1>
<h2 id="install">Install</h2>
```

## Syntax and placement

The defaults are `[TOC]` and `[[_TOC_]]`, matching the standalone markers described
in [GitLab's documentation](https://docs.gitlab.com/user/markdown/#table-of-contents).
Matching is case-sensitive. A marker must occupy its own line, with at most three
leading spaces and optional trailing spaces or tabs. It can interrupt a paragraph
and can appear in a block quote or list item. Code, raw HTML blocks, escaped
markers, inline occurrences and link reference definitions retain their ordinary
meaning. Without the extension, parsing is unchanged.

```kotlin
import io.heapy.kotmark.ext.toc.TocPlacement

val extensions = extensions {
    toc {
        markers = listOf("{{contents}}") // replaces both defaults
        headingLevels = 2..4             // inclusive; default 1..6
        placement = TocPlacement.PREPEND_IF_MISSING
    }
}
```

`MARKERS` (the default) generates no TOC when no marker exists.
`PREPEND_IF_MISSING` and `APPEND_IF_MISSING` add one insertion point at the start
or end of the document only when no marker exists in standard block containers.
Automatic insertion uses the first configured marker. Every explicit marker
renders the same complete outline. With no matching headings, HTML and text emit
nothing for a TOC. Markdown still preserves its marker.

The outline includes ATX and setext headings in document order, including those
inside standard block quotes and lists. Contents of custom blocks (such as
footnote definitions and alerts) are excluded: their renderers may hide, reorder
or defer those contents. Code and raw HTML do not contribute headings. Each
included heading nests under the nearest preceding included heading with a lower
level. Skipped levels create no empty list items. Filtering applies before
nesting, but all standard headings still participate in ID allocation.

## Renderers and heading IDs

- **HTML:** tight, nested unordered lists linking to headings. TOC supplies default
  heading IDs using the heading-anchor module's allocator when no attribute
  provider supplies an ID. IDs are added even when the document has no marker.
- **Markdown:** preserves each marker, including automatically inserted markers;
  it does not materialize links that another Markdown engine might resolve
  differently. Reparse with this extension to regenerate the outline.
- **Text:** a nested bullet outline with no URLs, respecting the renderer's line
  break mode. A marker rendered on its own uses its containing document's headings.

Labels are plain text collected from the heading AST: text, code, link labels,
image alt text and nested formatting contents are retained; line breaks become
spaces. Raw HTML tags and opaque custom inline literals are omitted. Formatting
remains intact in the original headings. Generated lists contain no headings or
TOC nodes and are never inserted into the source AST.

Use `headingAnchor { idPrefix = "section-" }` alongside `toc()` to configure IDs;
either extension order works. For custom ID generation, register an HTML
`attributeProviderFactory` after heading-anchor and assign `attributes["id"]` for
`Heading` nodes. The TOC renderer evaluates heading attributes once before HTML
output and reuses those exact attributes for headings and links. Custom providers
must supply nonempty, unique IDs and must not mutate the AST. Factories provide
per-render state; arbitrary shared callbacks are not made thread-safe.

The TOC extension owns HTML rendering of `Heading` and `TableOfContents`. The first
registered node renderer wins. An earlier custom heading renderer must emit the
same IDs, or it should also provide its own TOC renderer. Rendering an HTML subtree
collects only headings within that subtree. Hidden/custom block headings are
rendered normally when their owning renderer requests them, but are not TOC entries.

Heading labels and attributes are escaped. Literal IDs are encoded as URI
fragments (including `%` and `#`); generated links then use the ordinary HTML
resolve → sanitize → encode → escape pipeline. A URL resolver or attribute
provider that rewrites generated links is responsible for preserving their targets.
