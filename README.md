# kotmark

[![CI](https://github.com/Heapy/kotmark/actions/workflows/ci.yml/badge.svg)](https://github.com/Heapy/kotmark/actions/workflows/ci.yml)

Kotlin Multiplatform library for parsing CommonMark into a document tree and
rendering it as HTML, Markdown, or plain text. Based on
[commonmark-java](https://github.com/commonmark/commonmark-java), with opt-in
extensions and support for custom parsers and renderers.

Supports JVM (Java 17+), Android, JavaScript, WebAssembly, and Kotlin/Native.

The optional [kotlinx.html renderer](kotmark-renderer-kotlinx-html/README.md)
inserts Markdown into HTML DSL/DOM consumers with explicitly selected extension adapters.

## Quick start

Add the dependency to your module (or `commonMain.dependencies` in a Multiplatform project):

```kotlin
implementation("io.heapy.kotmark:kotmark:0.1.0")
```

```kotlin
import io.heapy.kotmark.parser.parser
import io.heapy.kotmark.renderer.html.htmlRenderer

val document = parser().parse("Hello, **Kotlin**!")
val html = htmlRenderer().render(document)
// <p>Hello, <strong>Kotlin</strong>!</p>
```

## Specification support

CommonMark 0.31.2, checked against the complete 655-example specification suite
and the 652-example corpus bundled with commonmark-java.
Extensions add tables, strikethrough, task lists, footnotes, math, and more.
Support for selected GFM features does not imply full GFM conformance.

## Documentation

[User guide](https://heapy.github.io/kotmark/) ·
[Extensions](https://heapy.github.io/kotmark/extensions/) ·
[Guides](https://heapy.github.io/kotmark/guides/configuration.html) ·
[API reference](https://heapy.github.io/kotmark/api/) ·
[Contributing](https://heapy.github.io/kotmark/contributing.html)

## License

Original Kotmark code and modifications: [Apache License 2.0](LICENSE).
Third-party licenses and notices: [UPSTREAM.md](UPSTREAM.md).
