# kotmark

[![CI](https://github.com/Heapy/kotmark/actions/workflows/ci.yml/badge.svg)](https://github.com/Heapy/kotmark/actions/workflows/ci.yml)

Kotlin Multiplatform port of [commonmark-java](https://github.com/commonmark/commonmark-java) for parsing and rendering CommonMark documents.

The repository contains the core `kotmark` module, shared test utilities, integration tests, and extensions for autolinks, footnotes, GFM alerts, strikethrough, tables, heading anchors, image attributes, insertions, task list items, and YAML front matter.

## Development

The project pins Kotlin Toolchain 0.13.0 in both wrappers and Kotlin 2.4.21 in
`common.module-template.yaml`. Run the JVM test suite with the project-local wrapper:

```shell
./kotlin test --platform jvm
```

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

GitHub Actions uses `Heapy/setup-ktc` for the pinned wrapper and shared/Native caches,
and `Heapy/ktc-check` for built-in tests and their available JUnit reports.
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

KTC 0.13 has no built-in JS or WASI runner, and its generic Native runner does not
launch tvOS/watchOS simulators. The custom runners fill those gaps and fail if no
tests execute. Device jobs compile production and test sources to klibs without
linking executables. The legacy `watchosArm32` (`armv7k`) and Intel iOS simulator
(`iosX64`) targets are not supported; Apple simulator coverage uses ARM64.
Device compilation is not runtime test coverage. The full upstream
fixture/integration suite remains JVM-only; other targets execute the common tests.

Linux ARM64 binaries are cross-compiled on x64 because Kotlin/Native does not
provide a Linux ARM64 compiler host. CI transfers them as an artifact and executes
them on an ARM64 runner. Apple jobs use the macOS 26 ARM64 image.

The JVM integration suite needs the pinned reference checkouts listed in
`.github/workflows/ci.yml`, under `projects/commonmark-java` and
`projects/commonmark-spec`. CI fetches them automatically.

Supplementary Unicode character categories are generated from Unicode 16.0.0.
To regenerate the checked-in ranges (requires Python 3 and network access):

```shell
python3 scripts/generate-unicode-categories.py
```
