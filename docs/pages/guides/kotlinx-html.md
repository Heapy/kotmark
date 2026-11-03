---
title: kotlinx.html renderer
nav: kotlinx.html renderer
kicker: Guide / structured HTML
description: Render kotmark nodes into a kotlinx.html consumer rather than an HTML string.
lede: Syntax extensions that create custom nodes need corresponding kotlinx.html adapters.
cards:
  - term: Artifact
    value: io.heapy.kotmark:kotmark-renderer-kotlinx-html:0.1.0
  - term: Package
    value: io.heapy.kotmark.renderer.kotlinxhtml
---

## Render into an article

Register an adapter for each extension whose nodes need specialized rendering.

```kotlin caption="Compose an article with adapters" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/KotlinxHtml.kt#kotlinx-html
```

```example example=kotlinx-html
Inline $x^2$.

| a | b |
| - | - |
| 1 | 2 |
```

## Adapter coverage

Core nodes have built-in support. For extension nodes, pass the matching adapter to `kotlinxHtmlRenderer`; an unhandled custom node is not silently converted to raw HTML.
