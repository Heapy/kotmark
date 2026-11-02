# kotmark

[![CI](https://github.com/Heapy/kotmark/actions/workflows/ci.yml/badge.svg)](https://github.com/Heapy/kotmark/actions/workflows/ci.yml)

Kotlin Multiplatform port of [commonmark-java](https://github.com/commonmark/commonmark-java) for parsing and rendering CommonMark documents.

The repository contains the core `kotmark` module, shared test utilities, integration tests, and extensions for autolinks, footnotes, GFM alerts, strikethrough, tables, heading anchors, image attributes, insertions, task list items, and YAML front matter.

The independent `autolink` module ports [autolink-java 0.12.0](UPSTREAM.md).
The Markdown autolink extension uses its upstream rules, which differ from GFM in some cases.

## Development

Run the complete JVM suite with `./kotlin test --platform jvm`.

The specification and integration suites are common Kotlin tests on every target:
both pinned CommonMark corpora (652 and 655 examples), the cmark and commonmark.js
regressions, Markdown round trips, GFM table and alert examples, and integration
with extensions and truncated inputs. Existing expected-output overrides are
shared across platforms; examples are not skipped for individual targets.
The GFM checks cover the supported sections, not full GFM conformance.

`test-fixtures` embeds all six committed corpus files as common Kotlin text.
Normal test runs need no upstream checkout, source download, resource loader or
generation step. The original bytes and revisions remain pinned in
[UPSTREAM.md](UPSTREAM.md). JVM integrity tests compare every embedded corpus with
the original resource and its SHA-256; common tests check the complete corpus counts.
Only tests of Java-specific APIs and the resource/SHA-256 integrity check remain
JVM-only. Fixture and test utility modules are test infrastructure, not dependencies
of the production parsing libraries.

After intentionally updating the pinned fixture files and checksums, regenerate
their committed Kotlin representation with:

```shell
kotlinr scripts/generate-test-fixtures.main.kts
```

Add `--check` to verify generation is current without changing files.

JVM artifacts target Java 17, and JVM tests run on JDK 17. Kotlin Toolchain
provisions a matching JDK when needed. The shared template also configures the
larger test thread stack required by deeply nested Markdown fixtures.

Run Native tests on the host platform (`linuxX64` on Linux or `macosArm64` on Apple Silicon):

```shell
./kotlin test --platform macosArm64
```

Run the common tests on JavaScript with Node.js 24 (the version used in CI):

```shell
node scripts/test-js.mjs
```

The Node runner links the test modules through the wrapper, then runs their `kotlin.test` suites.

Run Wasm-JS tests in the browser provisioned by Kotlin Toolchain, or Wasm-WASI tests
with Node.js 24:

```shell
./kotlin test --platform wasmJs
node scripts/test-additional-platforms.mjs wasmWasi
```

On Apple Silicon with the corresponding Xcode simulator runtimes installed:

```shell
./kotlin test --platform iosSimulatorArm64
node scripts/test-additional-platforms.mjs tvosSimulatorArm64
node scripts/test-additional-platforms.mjs watchosSimulatorArm64
```

### CI coverage

All declared targets are covered by the matrix:

| Targets | Runner | Verification |
| --- | --- | --- |
| `jvm`, `android` | Ubuntu x64 | JVM suite and Android local unit tests (no emulator) |
| `linuxX64` | Ubuntu x64 | Native tests |
| `linuxArm64` | Ubuntu x64 build → Ubuntu ARM64 execution | Native tests |
| `mingwX64` | Windows x64 | Native tests |
| `macosArm64`, `iosSimulatorArm64` | macOS ARM64 | Native and iOS simulator tests |
| `js` | Ubuntu x64, Node.js 24 | Custom JS runner |
| `wasmJs` | Ubuntu x64 | KTC Chromium runner |
| `wasmWasi` | Ubuntu x64, Node.js 24 | Custom WASI runner |
| `tvosSimulatorArm64`, `watchosSimulatorArm64` | macOS ARM64 | Custom simulator runner |
| `iosArm64`, `tvosArm64`, `watchosArm64`, `watchosDeviceArm64` | macOS ARM64 | Compilation only; execution requires devices |

## License

Original Kotmark code and modifications are licensed under the
[Apache License 2.0](LICENSE). Third-party code, data and test fixtures retain
their applicable licenses and notices, listed in [UPSTREAM.md](UPSTREAM.md).
