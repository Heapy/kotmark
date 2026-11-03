---
title: Migration from commonmark-java
nav: Migration
kicker: Guide / porting applications
description: Move a commonmark-java integration to kotmark's Kotlin Multiplatform API.
lede: The parser and AST preserve familiar concepts while Kotlin factory DSLs and multiplatform source sets replace Java-only wiring.
cards: []
---

## Start with factories

Replace Java builders with Kotlin factory functions and configuration lambdas. The configuration is snapshotted when each factory creates its parser or renderer.

```kotlin label="Create parser and HTML renderer"
val parser = parser { extensions(extensions) }
val renderer = htmlRenderer { extensions(extensions) }
```

## Block continuation

Port custom block parsers by returning `BlockContinue.AtIndex`, `AtColumn`, `Finished`, or `null`. `AtIndex` measures UTF-16 positions; `AtColumn` measures visual columns.

## Upstream lineage

Kotmark preserves upstream notices and behavioral contracts; review the [recorded source and license notices](../credits.md#upstream-notices) before comparing any dialect differences.

## Compatibility notes

- Keep parser and renderer extension registration aligned for extensions that add nodes.
- URL resolution is an HTML rendering step and does not mutate AST destinations.
- The first registered renderer for a node type wins; register overrides before extension defaults.
