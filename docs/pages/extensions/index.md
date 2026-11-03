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

Parser extensions are registered with `parser { extensions(...) }`. HTML-only extensions belong on the renderer; extensions that add syntax and output should be registered on both sides.