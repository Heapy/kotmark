---
title: Table of contents
nav: Table of contents
kicker: Extension 14 / kotmark-ext-toc
description: Insert a linked outline with standalone TOC markers.
lede: Add [TOC] or [[_TOC_]] on its own line to generate a nested outline from headings.
cards:
  - term: Artifact
    value: io.heapy.kotmark:kotmark-ext-toc:0.1.0
  - term: Package
    value: io.heapy.kotmark.ext.toc
---

## Usage

Register `toc()` with the parser and each renderer. Heading IDs are supplied by
default; combine it with [heading anchors](heading-anchor.md) to customize them.

```kotlin caption="Link an outline to heading IDs" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/Toc.kt#toc
```

## Example

```example example=toc
[TOC]

## Getting started
### Install
## Usage
```

## Configuration and behavior

Both `[TOC]` and `[[_TOC_]]` match case-sensitive standalone lines. Code blocks,
inline occurrences and escaped markers keep their ordinary meaning. Use
`markers = listOf("{{contents}}")` to replace the defaults. `headingLevels = 2..4`
selects an inclusive level range; the default is `1..6`.

The default `TocPlacement.MARKERS` requires an explicit marker. Choose
`PREPEND_IF_MISSING` or `APPEND_IF_MISSING` to insert one automatically. Multiple
markers each show the same outline. No matching headings means no HTML or text
outline. Skipped heading levels nest under the nearest preceding lower-level
entry, without empty list items.

HTML produces a nested unordered list with plain text labels and matching heading
IDs, including duplicates and Unicode. Markdown preserves the markers for later
regeneration. Plain text produces a bullet outline without links. Text, code,
link labels and image alt text contribute to labels; raw HTML tags and opaque
custom inline literals do not.

Headings in standard block quotes and lists are included. Custom block contents,
including footnote definitions and alerts, are excluded because their renderers
may hide or defer them. The extension owns HTML heading rendering; register custom
node renderers first and keep their IDs consistent with the TOC. Custom heading
attribute providers run once per heading before output and must provide unique,
nonempty IDs. Generated links use the normal URL resolution and escaping pipeline.
