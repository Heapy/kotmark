# Documentation

User documentation is published as one HTML site: start with the
[user guide](https://heapy.github.io/kotmark/) or browse the
[API reference](https://heapy.github.io/kotmark/api/). Its sources are in
[`pages`](pages) and the hand-written landing page and assets are in [`site`](site).
Compiled usage examples live in [`examples`](examples); the JVM site builder reads
the YAML page manifest with kotaml and renders Markdown using the current checkout.

## Using Kotmark

- [Getting started](https://heapy.github.io/kotmark/#start): installation and parsing Markdown.
- [Configuration](https://heapy.github.io/kotmark/guides/configuration.html): parser and renderer options, factories, and typed node renderers.
- [URL resolution](https://heapy.github.io/kotmark/guides/url-resolution.html): base URLs, routing, and HTML destinations.
- [Extensions](https://heapy.github.io/kotmark/extensions/): one page per syntax module, with a usage example.
- [Custom blocks](https://heapy.github.io/kotmark/#blocks) and [block continuation](https://heapy.github.io/kotmark/guides/block-continuation.html).
- [Custom renderers](https://heapy.github.io/kotmark/#renderer): override output for selected nodes.
- [kotlinx.html renderer](https://heapy.github.io/kotmark/guides/kotlinx-html.html): render into HTML DSL and DOM consumers.
- [Migration](https://heapy.github.io/kotmark/guides/migration.html): adapting builder-based APIs and existing extensions.

## Working on Kotmark

- [Contributing](https://heapy.github.io/kotmark/contributing.html): setup, checks, and the target matrix.
- [Development and verification](development.md): tests, API checks, fuzzing, and benchmarks in detail.
- [Site maintenance](maintaining-site.md): build, preview, checked examples, and generated reference pages.
- [Autolink test mapping](autolink-testing.md): correspondence with upstream tests.
- [Agent instructions](../AGENTS.md): repository rules and checks to run for each kind of change.
- [Upstream sources and notices](../UPSTREAM.md): pinned revisions and third-party licenses.
