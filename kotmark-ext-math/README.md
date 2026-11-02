# Math extension

`kotmark-ext-math` parses formulas as opaque source. It does not parse TeX,
execute macros, load scripts or styles, or typeset formulas.

```kotlin
import io.heapy.kotmark.ext.math.MathExtension
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

val extensions = listOf(MathExtension.create())
val parser = parser { extensions(extensions) }
val html = htmlRenderer { extensions(extensions) }
val document = parser.parse($$"""Energy: $E=mc^2$.""")
```

## Delimiter contract

This is a Kotmark grammar, not a claim of compatibility with a particular math
Markdown dialect. Math is enabled only when this extension is registered.

### Inline

- A single unescaped `$` opens a formula. Its next character must exist and must
  not be whitespace (`Char.isWhitespace`). Intraword formulas such as `a$x$b`
  are allowed.
- Only the nearest unescaped dollar run can close it. It must be exactly one
  `$`, preceded by a non-whitespace character and not followed by an ASCII digit
  (`0`–`9`). An invalid candidate rejects this opener; parsing resumes normally,
  so later dollar signs may open their own formulas.
- Content must be nonempty and fit on one source line. Runs of two or more `$`
  are literal inline text; no suffix of a run can open inline math.
- Inside a formula, an odd number of consecutive backslashes escapes the next
  dollar; an even number does not. All backslashes remain in the formula source.
  Outside formulas, normal Markdown backslash escaping applies.
- `$x$` and `$2$` are formulas. `$ x$`, `$x $`, `$x$2`, `$5 and $10`, `$$x$$`,
  `$$$x$$$` and an unmatched `$` stay text. To write literal dollar pairs such as
  `\$5\$`, escape them. Currency cannot be disambiguated in all natural text.

### Block

- An opening fence consists of exactly `$$` on its own line, optionally followed
  by spaces or tabs and preceded by up to three columns of indentation relative
  to the enclosing list or blockquote. Four-column indentation remains code.
- A closing fence follows the same rule. `$$x$$`, `$$$`, `$$ label`, and `\$$`
  are not fences. A block can interrupt a paragraph and can occur in lists or
  blockquotes. Blank lines are content.
- The block ends at its closing fence, at the enclosing container's end, or at
  end of input. An unclosed block remains a `MathBlock`, like fenced code.
- Container markers and up to the opening fence's indentation in spaces are
  removed from content lines. Remaining content is preserved, including blank
  lines, backslashes and trailing spaces. Line endings are normalized to LF and
  each content line has a trailing LF, including a final unterminated source line.
  Empty blocks have an empty literal. Fences are excluded from the literal.

```markdown
$$
\sum_{k=1}^n a_k^2
$$
```

Neither form parses Markdown, HTML, entities, links or nested math within its
content. Code spans/blocks and link destinations take precedence over inline
math; link labels can contain inline math. `\(…\)` and `\[…\]` are not math
delimiters. Source spans, when enabled, include delimiters (and block fences).
The core parser's normal input processing, including replacement of NUL, applies.

JetBrains Markdown in `projects/markdown` was inspected as a reference; the
revision is recorded in [UPSTREAM.md](../UPSTREAM.md). Its same-line display math,
longer dollar runs, word/punctuation boundary rules and recursively parsed
content are deliberately not this extension's grammar.

## Nodes and renderers

`InlineMath : CustomNode, Delimited` and `MathBlock : CustomBlock` expose mutable,
non-null `literal: String` properties. Parsed nodes have no children. Applications
can visit them with the existing custom-node visitor API or register a
`NodeRenderer` for `InlineMath::class` and `MathBlock::class`.

The default renderers registered by the extension behave as follows:

| Renderer | Inline | Block |
| --- | --- | --- |
| HTML | `<span class="math math-inline">…</span>` | `<div class="math math-block">…</div>` |
| Text | Formula source without delimiters | Formula source followed by a block separator |
| Markdown | `$…$` | `$$` fences on separate lines, including a closing fence for unclosed input |

HTML always escapes the formula source and honors attribute providers. It emits
no script or engine-specific wrapper. Markdown preserves parsed formula content
and escapes dollar signs in ordinary text to avoid accidentally creating math;
it canonicalizes fences and indentation. Manually constructed or edited nodes
must contain content representable by the delimiter contract for a Markdown
round trip (for example, no unescaped dollar in an inline literal or closing
fence line inside a block literal).

Register an integration renderer **before** `extensions(extensions)` because the
first renderer registered for a node type wins:

```kotlin
import io.heapy.kotmark.ext.math.InlineMath
import io.heapy.kotmark.ext.math.MathBlock
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.renderer.NodeRenderer

val customHtml = htmlRenderer {
    nodeRendererFactory { context ->
        object : NodeRenderer {
            override val nodeTypes = setOf(InlineMath::class, MathBlock::class)

            override fun render(node: Node) {
                val source = when (node) {
                    is InlineMath -> node.literal
                    is MathBlock -> node.literal
                    else -> return
                }
                // Replace with your engine adapter; this example safely emits source.
                context.writer.tag("code")
                context.writer.text(source)
                context.writer.tag("/code")
            }
        }
    }
    extensions(extensions)
}
```

KaTeX/MathJax adapters can choose their own HTML and asset loading. Compose
integrations can render the public nodes directly. No such dependency is imposed
by the parser or default renderers.
