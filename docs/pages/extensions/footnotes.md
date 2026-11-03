---
title: Footnotes
nav: Footnotes
kicker: Extension 02 / kotmark-ext-footnotes
description: Footnote references and definitions, plus optional inline notes.
lede: Footnote nodes are numbered by first reference and rendered as a list with back-references.
cards:
  - term: Artifact
    value: io.heapy.kotmark:kotmark-ext-footnotes:0.1.0
  - term: Package
    value: io.heapy.kotmark.ext.footnotes
---

## Usage

Register the extension with both parser and renderer; enable inline notes only if needed.

```kotlin caption="Footnotes with inline notes enabled" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/Footnotes.kt#footnotes
```

## Example

```example example=footnotes
Kotmark ports commonmark-java.[^port] Inline notes work too.^[Like this one.]

[^port]: Revisions are recorded upstream.
```

## Syntax

| Form | Meaning |
| --- | --- |
| `[^label]` | A reference to a footnote definition. |
| `[^label]: text` | A footnote definition block. |
| `^[text]` | An inline footnote when `inlineFootnotes = true`. |
