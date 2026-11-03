---
title: GFM alerts
nav: GFM alerts
kicker: Extension 03 / kotmark-ext-gfm-alerts
description: GitHub-style alert blocks for notes, tips, warnings, and custom types.
lede: Alerts parse as block quotes and use the alert renderer when the extension is enabled.
cards:
  - term: Artifact
    value: io.heapy.kotmark:kotmark-ext-gfm-alerts:0.1.0
  - term: Package
    value: io.heapy.kotmark.ext.gfm.alerts
---

## Usage

The built-in alert types are NOTE, TIP, IMPORTANT, WARNING, and CAUTION; applications can register additional names.

```kotlin caption="Add a custom alert type" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/Alerts.kt#gfm-alerts
```

## Example

```example example=gfm-alerts
> [!INFO]
> Extensions are opt-in.

> [!WARNING]
> Register them on both sides.
```

## Behavior

Alert blocks preserve their block-quote content. Custom types are registered with a display title and receive the corresponding `markdown-alert-*` class in HTML.
