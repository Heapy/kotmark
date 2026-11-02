# kotmark

[![CI](https://github.com/Heapy/kotmark/actions/workflows/ci.yml/badge.svg)](https://github.com/Heapy/kotmark/actions/workflows/ci.yml)

Kotlin Multiplatform port of [commonmark-java](https://github.com/commonmark/commonmark-java) for parsing and rendering CommonMark documents.

The repository contains the core `kotmark` module, shared test utilities, integration tests, and extensions for autolinks, footnotes, GFM alerts, strikethrough, tables, heading anchors, image attributes, insertions, task list items, and YAML front matter.

## Development

Run the JVM test suite with the project-local Kotlin Toolchain wrapper:

```shell
./kotlin test --platform jvm
```

Run Native tests on the host platform (`linuxX64` on Linux or `macosArm64` on Apple Silicon):

```shell
./kotlin test --platform macosArm64
```

Run the common tests on JavaScript with Node.js 22 or newer:

```shell
node scripts/test-js.mjs
```

The Node runner links the test modules through the wrapper, then runs their `kotlin.test` suites.
CI runs JVM, Linux Native, and JavaScript tests.

Supplementary Unicode character categories are generated from Unicode 16.0.0.
To regenerate the checked-in ranges (requires Python 3 and network access):

```shell
python3 scripts/generate-unicode-categories.py
```
