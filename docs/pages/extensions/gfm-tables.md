---
title: GFM tables
nav: GFM tables
kicker: Extension 05 / kotmark-ext-gfm-tables
description: Pipe tables with header rows, delimiter rows, and optional column alignment.
lede: The table extension adds pipe-table parsing and renderers for HTML, Markdown, and text.
cards:
  - term: Artifact
    value: io.heapy.kotmark:kotmark-ext-gfm-tables:0.1.0
  - term: Package
    value: io.heapy.kotmark.ext.gfm.tables
---

## Usage

```kotlin caption="Register table syntax and HTML output" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/Tables.kt#gfm-tables
```

## Example

```example example=gfm-tables
| Module | Kind |
| :--- | ---: |
| math | block |
| ins | inline |
```

## Syntax

- A header row must be followed by a delimiter row; column counts must match.
- Colons in the delimiter specify left, right, or center alignment.
- Cells support inline Markdown; escape a literal pipe as `\|`.
- The table ends at a blank line or when another block starts.
