---
title: Inserted text
nav: Inserted text
kicker: Extension 09 / kotmark-ext-ins
description: Parse ++text++ as inserted content.
lede: The inserted-text extension creates inline nodes and renders them with the HTML ins element.
cards:
  - term: Artifact
    value: io.heapy.kotmark:kotmark-ext-ins:0.1.0
  - term: Package
    value: io.heapy.kotmark.ext.ins
---

## Usage

```kotlin caption="Enable inserted text" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/Ins.kt#ins
```

## Example

```example example=ins
Kotmark is ++multiplatform++.
```

## Output

The HTML renderer emits `<ins>multiplatform</ins>`. This syntax is opt-in and does not alter ordinary plus signs.
