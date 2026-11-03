---
title: Highlight
nav: Highlight
kicker: Extension 07 / kotmark-ext-highlight
description: Mark highlighted inline text with the ==text== delimiter.
lede: Highlight parsing supports nested inline Markdown inside the marked span.
cards:
  - term: Artifact
    value: io.heapy.kotmark:kotmark-ext-highlight:0.1.0
  - term: Package
    value: io.heapy.kotmark.ext.highlight
---

## Usage

```kotlin caption="Enable highlighted text" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/Highlight.kt#highlight
```

## Example

```example example=highlight
==important **text**==
```

## Output

The HTML renderer wraps the highlighted inline content in `<mark>`. The syntax extension is opt-in and is also available to the Markdown renderer.
