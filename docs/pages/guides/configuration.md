---
title: Configuration
nav: Configuration
kicker: Guide / configuration
description: Configure kotmark parsers, renderers, and extensions with Kotlin factory functions.
lede: Parsers and renderers are created by Kotlin factory functions with a configuration block. Omitting the block uses the defaults.
cards: []
---

## Example

Build one extension list and pass it to the parser and renderer. Each extension is
created once, with the same settings used on both sides.

```kotlin caption="Share configured extensions" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/guides/ExtensionLists.kt#extensions-list
```

## Extension lists

Import `io.heapy.kotmark.extensions` for the list builder and registration functions
such as `tables` from the corresponding extension packages. Each function is
provided by its optional module; see the [extension index](../extensions/index.md).

Use `add(customExtension)` to register an existing extension, and
`addAll(existingExtensions)` to compose a list from another collection:

```kotlin
val combined = extensions {
    add(customExtension)
    addAll(existingExtensions)
    tables { maxCells = 10_000 }
}
```

The builder returns a read-only `List<Extension>` snapshot. It preserves insertion
order and duplicates; later changes to the builder or source collections do not
change the list. Extension objects and callbacks remain shared, so any mutable
state they hold is the caller's responsibility.

Register custom renderer overrides before extensions that supply default handlers:
the first registered renderer for a node type wins. Parsers and renderers use only
the extension interfaces they support, so a shared list can also contain parser-only
or renderer-only extensions. The kotlinx.html renderer uses its own
[adapters](kotlinx-html.md) for extension output.

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
