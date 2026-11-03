---
title: Configuration
nav: Configuration
kicker: Guide / configuration
description: Configure kotmark parsers, renderers, and extensions with Kotlin factory functions.
lede: Parsers and renderers are created by Kotlin factory functions with a configuration block. Omitting the block uses the defaults.
cards: []
---

## Example

Configure a parser and renderer with factory functions. Register syntax with the parser and the matching output extension with the renderer.

```kotlin caption="Configure a parser and renderer"
val parser = parser { extensions(listOf(TablesExtension.create())) }
val renderer = htmlRenderer { extensions(listOf(TablesExtension.create())) }
```

## Factories

Each factory creates an independent configuration. The core provides factories for `Parser`, `HtmlRenderer`, `MarkdownRenderer`, and `TextContentRenderer`; optional modules add their own extension factories.

## Typed node renderers

Use `nodeRenderer<Heading>` to register a handler whose node argument already has the correct type. Register overrides before the extension that supplies the default renderer.

## Configuration rules

- Register extensions with the parser and with each renderer that should handle them.
- Factory functions snapshot options and registrations when they create an instance.
- Registered callback objects remain shared; their mutable state is the caller's responsibility.