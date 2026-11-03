---
title: GFM strikethrough
nav: GFM strikethrough
kicker: Extension 04 / kotmark-ext-gfm-strikethrough
description: Parse GFM strikethrough delimiters as deleted text.
lede: The extension can require paired tildes so a single tilde remains ordinary text.
cards:
  - term: Artifact
    value: io.heapy.kotmark:kotmark-ext-gfm-strikethrough:0.1.0
  - term: Package
    value: io.heapy.kotmark.ext.gfm.strikethrough
---

## Usage

Set `requireTwoTildes` to require `~~text~~` instead of accepting single tildes.

```kotlin caption="Require two tildes" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/Strikethrough.kt#gfm-strikethrough
```

## Example

```example example=gfm-strikethrough
~~Old~~ new, and ~one tilde~ stays.
```

## Output

The HTML renderer writes deleted text as `<del>`. The Markdown renderer retains the strikethrough delimiters.
