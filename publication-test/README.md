# Published renderer integration tests

These consumers test the real `kotmark-renderer-kotlinx-html` publication. They
are intentionally outside the root project's modules: every Kotmark dependency
must resolve from Maven artifacts, never from a sibling project module.

Run from the repository root with Kotlin 2.4.21 (`kotlinr`), JDK 25 and the usual
Kotlin Toolchain platform tooling (Node.js 24 for JS/WASI, provisioned Chromium
for Wasm-JS, Xcode for macOS). The runner uses only the Kotlin/JDK standard libraries:

```shell
kotlinr scripts/test-publication.main.kts
# Select targets explicitly; repeat the option to select several.
kotlinr scripts/test-publication.main.kts --platform jvm --platform js
kotlinr scripts/test-publication.main.kts --platform macosArm64
```

Defaults are JVM, JS, Wasm-JS, Wasm-WASI and the runnable Native target on macOS
ARM64 or Linux x64. CI runs these targets separately. A failing publication,
dependency contract or consumer test makes the command fail.

## What is published

The runner stages current production sources and module dependency declarations,
including their scopes, under `build/publication-test/<platform>/producer`.
It follows local dependencies recursively. It narrows the target list and source
sets to one target per run, maps applicable source-set aliases to that target,
omits producer tests and tooling plugins, substitutes local publication
coordinates, and disables Central publication and artifact signing.
Production dependencies and compiler settings come from the actual
module files and shared templates; there is no duplicate renderer implementation.

Artifacts are published only to Maven Local under
`io.heapy.kotmark.publicationtest`, with content-derived versions that include
production sources, module files, templates, the wrapper and runner. This avoids
stale Maven caches and does not overwrite release coordinates. The separate
`consumer` project references those coordinates and copies the tests in this
folder. No remote publication or credentials are needed.

This checks individual platform publications with Kotlin Toolchain consumers.
It does not validate combined multi-target publication metadata or Gradle/Maven
consumer builds.

## Consumer contracts

| Consumer | Explicit Kotmark dependencies | Checks |
| --- | --- | --- |
| `without-renderer` | core, math | Existing parser/string renderer work; HTML renderer, kotlinx.html and other extensions are absent. |
| `renderer-only` | core, HTML renderer | Plain Markdown works without registering adapters. JVM also checks that adapter classes are packaged while extension classes are absent. |
| `with-math` | core, HTML renderer, math | Inline/block math, escaping and explicit registration work without selecting other extensions. |
| `with-highlight` | core, HTML renderer, highlight | Highlight works without selecting other extensions. |

Dependency graphs include compilation, runtime and test scopes. Test fixtures and
jsoup must never leak into consumers. JVM/JS/Wasm-JS renderer consumers must not
resolve unselected extensions. On Wasm-WASI only `without-renderer` runs: the
renderer deliberately does not support that target because kotlinx.html 0.12.0's
advertised WASI artifact is missing from Maven Central.

### Native dependency limitation

With Kotlin Toolchain 0.13.0 and Kotlin 2.4.21, Native publications turn the
renderer's `compile-only` extension dependencies into transitive dependencies.
The renderer KLIB also records them in its dependency manifest. Native consumers
may therefore resolve every supported extension even if they register only one
adapter. Keeping the adapters inside the renderer still isolates parser-only
consumers from HTML dependencies.

The test accepts these transitive extensions **only for Native renderer
consumers**, and records `unselected_extensions` separately in the report.
Successful rendering on Native does not establish dependency isolation. If a
future toolchain stops pulling in unused extensions, the test still passes with
an empty list. DCE and release binary size are separate concerns and are not
measured here.

## Diagnostics

`build/publication-test/<platform>/` contains publication/dependency/test logs,
`results.json`, and published POM/module metadata plus KLIB dependency manifests
under `metadata/`. The aggregate report is `build/publication-test/results.json`.
CI uploads these diagnostics even on failure. Generated projects and reports can
be removed by deleting `build/publication-test`; local test artifacts live under
`~/.m2/repository/io/heapy/kotmark/publicationtest/`.
