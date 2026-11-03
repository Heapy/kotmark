---
title: Math
nav: Math
kicker: Extension 10 / kotmark-ext-math
description: Parse inline and display math source without depending on a TeX renderer.
lede: Kotmark preserves formula text and emits semantic wrappers; rendering equations is left to the host application.
cards:
  - term: Artifact
    value: io.heapy.kotmark:kotmark-ext-math:0.1.0
  - term: Package
    value: io.heapy.kotmark.ext.math
---

## Usage

```kotlin caption="Enable inline and block math" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/Math.kt#math
```

## Example

```example example=math
Energy: $E=mc^2$.

$$
\sum_{k=1}^n a_k^2
$$
```

## Custom HTML output

Override the extension's default math node renderer to emit `<code>` elements or connect a client-side math renderer.

```kotlin caption="Render formulas as code" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/MathRenderer.kt#math-renderer
```

```example example=math-renderer
Energy: $E=mc^2$.
```
