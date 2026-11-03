# Development and verification

Commands in this document run from the repository root. For library usage, start
with the [user documentation](README.md).

Restore the pinned build plugins after cloning, before invoking Kotlin Toolchain:

```shell
./ktc-plugins sync
./ktc-plugins verify
```

The release launchers verify the installer binary. `ktc-plugins.yaml` selects plugin
releases, and `ktc-plugins.lock.yaml` pins their exact source commits and hashes.
Plugins use **downloaded** mode: their sources and licenses are restored under
ignored `plugins/` directories, never vendored. CI restores and verifies the same lock.

Run the complete JVM suite with `./kotlin test --platform jvm`.

## Property tests and fuzzing

The common integration suite includes deterministic property tests and mutation fuzz
smoke tests for the core parser and all extensions together. It checks generated
Markdown round trips, line-ending/NUL normalization, parser reuse, AST links,
source-span bounds, and all three renderers. Arbitrary UTF-16 (including lone
surrogates), mutations of pinned CommonMark examples, and truncated extension
syntax exercise malformed input. These tests use `kotlin.random.Random` and
`kotlin.test`, so they run on every supported target without a JVM-only property
framework. Each run checks 1,500 generated cases plus all prefixes of the seed set.

```shell
./kotlin test --platform jvm -m integration-test
```

Seeds and iteration counts are in
[`GeneratedMarkdownTest.kt`](../integration-test/test/robustness/GeneratedMarkdownTest.kt).
Failures report the seed, iteration, original input, and a bounded deletion-shrunk
reproducer as UTF-16 escapes. Paste a reproducer into a focused regression test;
rerun the same test to replay its fixed seed. VM errors escape immediately rather
than being retried by the shrinker. Round-trip equality is checked on a bounded
valid Markdown grammar; arbitrary malformed input is checked for structural and
rendering consistency.

For coverage-guided JVM fuzzing, the separate `fuzz` app uses pinned
[Jazzer 0.30.0](https://github.com/CodeIntelligenceTesting/jazzer/tree/v0.30.0).
It decodes mutated bytes as UTF-8 and runs the shared invariants with core syntax
and all extensions, across all source-span modes. Jazzer dependencies belong only
to the fuzz app. CI runs a 60-second campaign and uploads `build/fuzz/` with its
corpus and any crash artifacts.

```shell
# Default 60-second campaign, seeded from common extension examples.
./kotlin run -m fuzz --jvm-args="-Xmx1024m -Xss4m"

# Longer campaign; the corpus persists between runs.
./kotlin run -m fuzz --jvm-args="-Xmx1024m -Xss4m" -- -max_total_time=600 build/fuzz/corpus

# Replay one saved crash, or minimize it (replace HASH with the actual filename).
./kotlin run -m fuzz --jvm-args="-Xmx1024m -Xss4m" -- build/fuzz/crash-HASH
./kotlin run -m fuzz --jvm-args="-Xmx1024m -Xss4m" -- -minimize_crash=1 build/fuzz/crash-HASH
```

Explicit arguments replace the default duration and corpus arguments; include a
corpus directory for campaigns. Defaults cap inputs at 4 KiB, each input at
10 seconds, and RSS at 2 GiB; Jazzer/libFuzzer flags can override these limits.
Build outputs are ignored by Git. Promote minimized findings into committed
regression tests before cleaning `build/`. A bounded smoke run supplements the
specification suite; it does not establish absence of parser bugs.

## Conformance and platform tests

The specification and integration suites are common Kotlin tests on every target:
both pinned CommonMark corpora (652 and 655 examples), the cmark and commonmark.js
regressions, Markdown round trips, GFM table and alert examples, and integration
with extensions and truncated inputs. Existing expected-output overrides are
shared across platforms; examples are not skipped for individual targets.
The GFM checks cover the supported sections, not full GFM conformance.

`test-fixtures` embeds all six committed corpus files as common Kotlin text.
Normal test runs need no upstream checkout, source download, resource loader or
generation step. The original bytes and revisions remain pinned in
[UPSTREAM.md](../UPSTREAM.md). JVM integrity tests compare every embedded corpus with
the original resource and its SHA-256; common tests check the complete corpus counts.
Only tests of Java-specific APIs and the resource/SHA-256 integrity check remain
JVM-only. The `test-fixtures` module also provides the shared example reader used by core,
extension, and integration suites. It is test infrastructure, not a dependency
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

## Library tooling

Production libraries apply `library.module-template.yaml`; test infrastructure and
benchmarks use the common template without API, documentation, or coverage policy.
The library template supplies `io.heapy.kotmark:…:0.1.0-SNAPSHOT` metadata for Dokka's
KMP model. These local snapshot coordinates are separate from the published
`0.1.0` release used in the user guides.

**API review:** BCV checks committed JVM and KLib snapshots. Run checks before and
after changing public declarations; update snapshots only after reviewing the change:

```shell
node scripts/library-tools.mjs api-check
node scripts/library-tools.mjs api-dump kotmark
```

The optional module arguments select production libraries; omitting them selects
all libraries. The helper runs modules sequentially because these plugins launch
nested builds. `jvm-api-check`, `jvm-api-dump`, `klib-api-check`, and `klib-api-dump`
select one snapshot format. Compatible additions also require baseline review.
KLib checks select web targets and the current host's Native family. Run on macOS,
Linux, and Windows for complete supported-target coverage; updates preserve other
targets already in the baseline. BCV 0.2.1 excludes Android and `watchosDeviceArm64`;
their existing platform builds/tests remain enabled. Swift/TypeScript exports and
behavioral compatibility are not checked.

For this pure-Kotlin project, macOS can also refresh all 13 supported KLib targets
by temporarily setting `plugins.bcv.includeCrossTargets: true` in the library
template before `api-dump`. This compiles each target's KLib. Remove the override
afterwards so routine checks select the host's targets again.

**Coverage:** on macOS or Linux, with JDK 25 and Kotlin 2.4.21+ providing `kotlinr` on PATH, run:

```shell
kotlinr plugins/kover/coverage.main.kts
./kotlin test --platform jvm -m test-fixtures -m integration-test
```

The first command executes each library's JVM tests once with instrumentation and
produces `build/tasks/*_koverReport@kover/html/index.html` and `coverage.xml` reports.
Add `-m kotmark` to inspect one library. The initial minimum is zero so reports can
guide a later threshold; empty coverage or zero tests still fails. Reports are per
module: tests in `integration-test` do not add to core's coverage. Native, Android,
and web coverage are outside this adapter's scope. CI uses this runner instead of
repeating ordinary library JVM tests. An unfiltered `./kotlin check` also runs
standalone Kover checks and therefore executes those tests again.

**API documentation:** generate one KMP site per library on a host with the required
Native SDKs (macOS with Xcode for this project):

```shell
node scripts/library-tools.mjs docs
```

Open `build/documentation/tasks/_<module>_dokkaHtml@dokka/html/index.html`. The helper
creates a disposable source/model copy under `build/documentation-project`, omitting
Android and mapping shared JVM/Android sources to JVM. It also fixes a Dokka 0.2.0
validator bug in the disposable plugin copy: package directories such as
`io.heapy.kotmark.renderer.html` must not be read as HTML files. The helper guards
the patch against source changes on upgrade. Production targets and the original
locked plugin installation remain unchanged and verifiable.
Sites include public and protected API. See [site maintenance](maintaining-site.md)
for assembling the published documentation. The manual CI workflow run uploads
per-library sites as `api-documentation`. External documentation links are
disabled by the adapter's default offline mode.

**Performance:** benchmarks separate parsing, rendering a prepared AST, combined
parse/render, fully consumed autolink extraction, and pathological input scaling.
Input preparation is outside measurement. Run a smoke check or actual measurements:

```shell
./kotlin do benchmarkSmoke -m benchmark-jvm
./kotlin do benchmark -m benchmark-jvm
```

Other runners are `benchmark-macos`, `benchmark-linux`, `benchmark-windows`,
`benchmark-js`, `benchmark-wasm-js`, and `benchmark-wasm-wasi`. Native runners need
matching hosts; web runners need Node.js 24. The benchmark library uses JDK 25 to
load the plugin's KSP processor, while JVM benchmark execution remains on JDK 17.
Results and run metadata are under
`build/tasks/_<runner>_benchmark@benchmark/` (or `benchmarkSmoke@benchmark`). Copy
reports before rerunning: each command replaces its previous report. Smoke checks
validate integration, not performance. Compare measurements on controlled hardware;
there is no automatic regression threshold. Benchmark classes live under
`benchmarks/src` and never enter production library dependencies.

## CI coverage

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

## Documentation site

See [site maintenance](maintaining-site.md) for preview, snippet checks, and report assembly.

Generated [JVM coverage reports](https://heapy.github.io/kotmark/coverage/) are per
module; they measure exercised lines, not specification conformance.
