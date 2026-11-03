---
title: Heading anchors
nav: Heading anchors
kicker: Extension 06 / kotmark-ext-heading-anchor
description: Add stable IDs to rendered heading elements.
lede: Heading anchors are an HTML-rendering feature; the parser needs no extension.
cards:
  - term: Artifact
    value: io.heapy.kotmark:kotmark-ext-heading-anchor:0.1.0
  - term: Package
    value: io.heapy.kotmark.ext.heading.anchor
---

## Usage

Configure a prefix and a fallback ID for headings whose text has no slug characters.

```kotlin caption="Customize heading IDs" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/HeadingAnchor.kt#heading-anchor
```

## Example

```example example=heading-anchor
# Getting started
## Getting started
#
```

## ID generation

Heading text is normalized to lowercase, punctuation is removed, and repeated IDs get a numeric suffix. The extension can add a prefix and suffix to each generated value.
