---
title: Block continuation
nav: Block continuation
kicker: Guide / custom block parsing
description: Continue, finish, or reject a custom block parser on each input line.
lede: BlockContinue makes a parser's line-consumption and indentation behavior explicit.
cards: []
---

## Continuation decisions

Return `AtIndex` for a UTF-16 input position, `AtColumn` for a visual column, `Finished` to consume the closing line, or `null` to let the enclosing parser handle it.

```kotlin caption="A parser with a closing line and indentation" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/guides/BlockContinuation.kt#block-continuation
```

## Edge cases

- Return `Finished` when the current line closes the block; the enclosing parser consumes that line.
- `null` is different from `Finished`: it gives the enclosing parser a chance to process the line.
- Tabs affect visual columns, while `AtIndex` positions remain UTF-16 offsets.
- Use the complete custom block guide on the home page for a `BlockParserFactory` and extension registration example.
