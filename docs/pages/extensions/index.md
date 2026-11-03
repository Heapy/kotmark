---
title: Extensions
nav: All extensions
kicker: Extension index / opt-in syntax
description: Optional syntax and renderer modules for kotmark.
lede: Keep the CommonMark core focused and add only the modules an application needs.
cards: []
---

## Extension modules

| Module | Adds |
| --- | --- |
| [Autolink](autolink.md) | Bare URL and email links |
| [Emoji](emoji.md) | Unicode emoji from `:shortcode:` aliases |
| [Footnotes](footnotes.md) | Footnote references and definitions |
| [GFM alerts](gfm-alerts.md) | GitHub-style alert blocks |
| [GFM strikethrough](gfm-strikethrough.md) | Struck-through text |
| [GFM tables](gfm-tables.md) | Pipe tables |
| [Heading anchors](heading-anchor.md) | Stable heading IDs |
| [Highlight](highlight.md) | `==highlighted text==` |
| [Image attributes](image-attributes.md) | Extra image attributes |
| [Inserted text](ins.md) | Inserted text syntax |
| [Math](math.md) | Inline and block formula source |
| [Task list items](task-list-items.md) | Checkbox list items |
| [YAML front matter](yaml-front-matter.md) | YAML document metadata |

## Registration

Create one list with `io.heapy.kotmark.extensions { … }`, then pass it to
`parser { extensions(list) }` and each renderer that needs it. Import the registration
functions from the packages listed on the individual extension pages.

```kotlin caption="Share configured extensions" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/guides/ExtensionLists.kt#extensions-list
```

| Module | Registration inside `extensions { … }` |
| --- | --- |
| Autolink | `autolink { linkTypes = setOf(AutolinkType.URL) }` |
| Emoji | `emoji()` or `emoji(customAliases = mapOf("party" to "🥳"))` |
| Footnotes | `footnotes { inlineFootnotes = true }` |
| GFM alerts | `alerts { addCustomType("INFO", "Information") }` |
| GFM strikethrough | `strikethrough { requireTwoTildes = true }` |
| GFM tables | `tables { maxCells = 100_000 }` |
| Heading anchors | `headingAnchor { idPrefix = "section-" }` |
| Highlight | `highlight()` |
| Image attributes | `imageAttributes()` |
| Inserted text | `ins()` |
| Math | `math()` |
| Task list items | `taskListItems()` |
| YAML front matter | `yamlFrontMatter()` or `yamlFrontMatter(factory)` |

Configuration blocks are optional. Use `add(customExtension)` for a custom instance
or `addAll(existingExtensions)` to compose lists. Registration order is preserved;
the first registered renderer for a node type wins. See
[configuration](../guides/configuration.md#extension-lists) for snapshot and sharing rules.

Parser-only and renderer-only extensions can share the same list: each consumer
applies only the extension interfaces it supports. The kotlinx.html renderer needs
its matching [adapters](../guides/kotlinx-html.md) for extension output.
