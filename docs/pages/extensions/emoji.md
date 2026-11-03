---
title: Emoji
nav: Emoji
kicker: Extension 13 / kotmark-ext-emoji
description: Turn emoji shortcodes into Unicode while preserving their Markdown spelling.
lede: "Write :monkey:, :star2:, or :speech_balloon: and render Unicode emoji without image downloads."
cards:
  - term: Artifact
    value: io.heapy.kotmark:kotmark-ext-emoji:0.1.0
  - term: Package
    value: io.heapy.kotmark.ext.emoji
---

## Usage

```kotlin caption="Enable emoji shortcodes" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/Emoji.kt#emoji
```

## Example

```example example=emoji
A :bug: fixed, a :star2: earned. People :heart: that!
```

## Syntax and output

Shortcodes are case-sensitive and must occur on one line. The bundled GitHub gemoji
dictionary includes aliases such as `:+1:` and `:thumbsup:`. Unknown codes remain
ordinary text. Codes can be adjacent or embedded in text; no surrounding spaces
are required. Escape a colon (`\:heart:`) to keep a known code literal.

Shortcodes work in emphasis, link labels, and image descriptions. They are not
expanded in code spans, code blocks, HTML tags/attributes, HTML blocks, link
destinations, or link titles. Inline HTML content between tags remains Markdown.

HTML and plain-text renderers output Unicode. Actual glyphs depend on the reader's
font and platform. The Markdown renderer preserves the original shortcode and
escapes ordinary text colons to avoid turning literal codes into emoji on reparse.
Pass the same extension configuration to the parser and renderers.

## Custom aliases

Pass a map to `emoji(customAliases = mapOf("party" to "🥳"))` to add entries or
override existing ones. Keys omit the colons and contain lowercase ASCII letters,
digits, underscores, plus or minus signs. Values are nonempty, single-line text.
The extension copies this map when it is created.

Each `Emoji` node records its `shortcode` and contains its rendered text as a `Text`
child. A custom HTML renderer can register `nodeRenderer<Emoji>` before the extension
to choose another presentation. Unicode remains available to visitors that collect
text, including image alt text.

The dictionary comes from [GitHub gemoji](https://github.com/github/gemoji).
Its MIT notice is included in [credits](../credits.md).
