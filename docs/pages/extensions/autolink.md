---
title: Autolink
nav: Autolink
kicker: Extension 01 / kotmark-ext-autolink
description: Detect bare URLs, www addresses, and email addresses while parsing Markdown.
lede: Autolinks are converted into ordinary link nodes, so the feature works with every renderer.
cards:
  - term: Artifact
    value: io.heapy.kotmark:kotmark-ext-autolink:0.1.0
  - term: Package
    value: io.heapy.kotmark.ext.autolink
---

## Usage

Add the extension module and register it with the parser.

```kotlin caption="Detect links while parsing" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/Autolink.kt#autolink
```

## Example

```example example=autolink
Visit https://example.com or write to team@example.com.
```

## Behavior

- Code spans and code blocks are not scanned.
- Only lowercase `www` is recognized.
- The standalone `autolink` artifact also provides link extraction for plain text.

## GFM differences {#gfm-differences}

The extractor follows autolink-java rules. These differ from GFM in three examples from its Autolinks specification section:

| Example | Difference |
| --- | --- |
| 5 | An unmatched closing parenthesis stops the link even when more URL text follows. |
| 6 | `&hl` remains inside the link while its final semicolon stays outside. |
| 11 | An email prefix before a trailing `-` or `_` is linked. |

## Extract plain-text links

The standalone `autolink` module extracts spans without parsing Markdown.

```kotlin caption="Extract URL, www, and email spans" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/extensions/LinkExtractor.kt#link-extractor
```

```example example=autolink-extractor input="INPUT · Plain text" input-language="text" output="OUTPUT · Link spans" output-language="text"
See www.kotlinlang.org and https://example.com.
```