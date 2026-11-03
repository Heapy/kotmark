---
title: YAML front matter
nav: YAML front matter
kicker: Extension 12 / kotmark-ext-yaml-front-matter
description: Parse an initial YAML metadata block as a separate document node.
lede: Front matter remains available to the application while HTML output contains the Markdown body only.
cards:
  - term: Artifact
    value: io.heapy.kotmark:kotmark-ext-yaml-front-matter:0.1.0
  - term: Package
    value: io.heapy.kotmark.ext.front.matter
---

## Usage

Visit the parsed document with `YamlFrontMatterVisitor` to read the top-level metadata.

```kotlin caption="Read metadata and render the body" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/YamlFrontMatter.kt#yaml-front-matter
```

## Example

`readPage` returns metadata and HTML separately. The map below is the extracted
metadata; only the Markdown body appears in the HTML output.

```example example=yaml-front-matter
---
title: Release notes
tags:
  - kotlin
  - markdown
---

# Hello
```

## Behavior

The extension recognizes only front matter at the beginning of a document. It does not validate YAML application schemas; use the returned values to apply your own validation and defaults.
