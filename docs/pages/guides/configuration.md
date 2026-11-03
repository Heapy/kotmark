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

## Block nesting limit

`maxOpenBlockParsers` defaults to `100`. Once the limit is reached, additional
block starts are treated as plain text instead of creating deeper block structure.
The document root is not counted, and reaching the limit does not throw an exception.

Set `parser { maxOpenBlockParsers = Int.MAX_VALUE }` to remove the limit, or choose
another non-negative value. This option controls block nesting; it does not limit
inline nesting or the total number of blocks in a document.

## Inline nesting limit

`maxInlineNesting` defaults to `100`. It tracks combined nesting from emphasis-like
delimiters, bracketed links, and images. Markers that would exceed the limit remain
plain text. Set `parser { maxInlineNesting = 3 }` to choose another non-negative
limit, or use `Int.MAX_VALUE` to disable it.

Emphasis inside a link is processed after the link's limit check, so that
combination can exceed the configured limit by one level. Custom delimiter
processors participate when they insert their new node directly after the opener;
arbitrary subtrees supplied by custom extensions are not fully measured.

Disabling the limit permits deep trees that recursive renderers and visitors may
not be able to traverse.

## Configuration rules

- Register extensions with the parser and with each renderer that should handle them.
- Factory functions snapshot options and registrations when they create an instance.
- Registered callback objects remain shared; their mutable state is the caller's responsibility.
