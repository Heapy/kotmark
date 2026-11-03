# Highlight extension

Add the `kotmark-ext-highlight` module and build an extension list with `highlight()`. Register it
with the parser and each renderer you use:

```kotlin
import io.heapy.kotmark.ext.highlight.highlight
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

val extensions = extensions { highlight() }
val parser = parser { extensions(extensions) }
val html = htmlRenderer { extensions(extensions) }
html.render(parser.parse("==important **text**=="))
// <p><mark>important <strong>text</strong></mark></p>
```

Both delimiter runs must contain exactly two unescaped `=` characters. Single,
triple and longer runs do not delimit highlights. Normal CommonMark left/right
flanking rules apply: `== text==` and `==text ==` stay text; intraword
`a==text==b` works. `==a====b==` is one highlight containing `a====b`.
Escaping either opening `=` prevents it from opening a highlight.

Content is parsed as Markdown, including emphasis, links, code spans and soft
line breaks. A highlight cannot cross a block boundary. Code spans/blocks and
link destinations are not scanned for highlights. Existing block rules still
take precedence: `title` followed by a line `==` is a Setext heading.

`Highlight` is a `CustomNode` implementing `Delimited`; its children and source
spans are available through the regular node API. HTML renders `<mark>` and
honors attribute providers. Text rendering visits the children; Markdown
rendering writes `==` around them and escapes literal equals signs in text.
As with other delimiter extensions, Markdown rendering is canonical rather than
byte-for-byte preservation of the input.

The exact-two delimiter behavior was checked against JetBrains Markdown's
highlight examples at the revision recorded in [UPSTREAM.md](../UPSTREAM.md).
