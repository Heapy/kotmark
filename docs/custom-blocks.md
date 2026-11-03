# Custom block continuation

Use this reference alongside the [custom block tutorial](https://heapy.github.io/kotmark/#blocks).

`BlockParser.tryContinue` returns `BlockContinue?`. Its non-null outcomes are
sealed and can be handled exhaustively:

| Outcome | Meaning |
| --- | --- |
| `BlockContinue.AtIndex(newIndex)` | Continue at an absolute UTF-16 index in the current line. |
| `BlockContinue.AtColumn(newColumn)` | Continue at an absolute visual column, accounting for expanded tabs. |
| `BlockContinue.Finished` | Close the block and its open children, consuming the current line. |
| `null` | The block does not continue; the enclosing parser handles the line, including possible lazy continuation. |

An extension can return these outcomes directly:

```kotlin
override fun tryContinue(state: ParserState): BlockContinue? = when {
    state.line.content.toString() == "END" -> BlockContinue.Finished
    state.indent >= 4 -> BlockContinue.AtColumn(state.column + 4)
    state.isBlank -> BlockContinue.AtIndex(state.index)
    else -> null
}
```

The existing `atIndex`, `atColumn`, `finished`, and `none` companion factories
remain available with their original signatures, including JVM static access.
Extensions using these factories and the nullable return type need no signature changes.
Indices and columns must be non-negative; constructors, factories, and `copy`
reject negative values with `IllegalArgumentException`.

See [migration](migration.md#block-continuation) when updating existing extensions.
