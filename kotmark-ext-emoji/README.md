# Emoji

Artifact: `io.heapy.kotmark:kotmark-ext-emoji:0.1.0` (planned first release).

```kotlin
import io.heapy.kotmark.ext.emoji.emoji
import io.heapy.kotmark.extensions
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

val extensions = extensions { emoji() }
val document = parser { extensions(extensions) }.parse("Hello :monkey: :star2:")
val html = htmlRenderer { extensions(extensions) }.render(document)
// <p>Hello 🐒 🌟</p>
```

The bundled dictionary contains 1,913 aliases for 1,870 Unicode emoji entries from
GitHub gemoji. All targets use the same embedded dictionary with no runtime I/O.
See [UPSTREAM.md](../UPSTREAM.md#emoji-dictionary) for the pinned revision and MIT
notice; the notice is also packaged in `META-INF/LICENSE-gemoji.txt`.

## Contract

- Shortcodes are case-sensitive, single-line `:name:` sequences. Names use
  lowercase ASCII letters, digits, `_`, `+`, or `-`. They can be adjacent and do
  not require surrounding spaces.
- Unknown and incomplete codes remain text. Escape a colon to keep a known code
  literal: `\:heart:`. Unicode emoji in the input are already ordinary text.
- Inline code, code blocks, HTML tags/attributes and HTML blocks are opaque.
  Link destinations and titles are not changed; link labels and image alt text
  support emoji. Angle-bracket autolinks are also opaque.
- HTML and text renderers emit Unicode, with HTML escaping where required.
  Glyph appearance depends on the host platform and font.
- Markdown rendering retains the original alias. Ordinary text colons are
  escaped to keep literal shortcodes literal when reparsed. Use the same dictionary
  for parsing the output; custom aliases are not serialized into the document.
- `Emoji(shortcode, unicode)` has a `Text` child holding the visible text. Source
  spans on the emoji node cover the original shortcode, including colons. The
  generated Unicode child does not claim source coordinates. Markdown output uses
  the alias even if the text child is edited; replace the emoji with `Text` when
  the replacement is no longer represented by that alias.

Add or override mappings with
`emoji(customAliases = mapOf("party" to "🥳"))`. Values must be nonempty and
single-line. The supplied map is copied when the extension is created.

Register custom renderers before the extension; the first renderer for a node
type wins. The standard HTML, Markdown and text renderers are supported. Other
renderers can handle `Emoji` explicitly or traverse its text child.

## Dictionary maintenance

`data/gemoji.json` is the pinned upstream input. Regenerate the committed common
Kotlin lookup with `node scripts/generate-emoji.mjs`, or verify it without changes
with `node scripts/generate-emoji.mjs --check`. Both commands work offline and
validate the input SHA-256. When updating the input, update its revision, hash and
counts in the generator and provenance record, then review the generated diff.
