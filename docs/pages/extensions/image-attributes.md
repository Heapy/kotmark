---
title: Image attributes
nav: Image attributes
kicker: Extension 08 / kotmark-ext-image-attributes
description: Add supported HTML attributes to Markdown image syntax.
lede: The extension recognizes a constrained attribute list so image sizing can be expressed next to the destination.
cards:
  - term: Artifact
    value: io.heapy.kotmark:kotmark-ext-image-attributes:0.1.0
  - term: Package
    value: io.heapy.kotmark.ext.image.attributes
---

## Usage

```kotlin caption="Enable image attributes" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/ImageAttributes.kt#image-attributes
```

## Example

```example example=image-attributes
![Logo](logo.svg){width=120 height=40}
```

## Supported values

The extension supports `width`, `height`, and `class` attributes. Renderers should apply these values as escaped attributes rather than interpolating them into markup.
