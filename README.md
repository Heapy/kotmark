# kotmark

[![CI](https://github.com/Heapy/kotmark/actions/workflows/ci.yml/badge.svg)](https://github.com/Heapy/kotmark/actions/workflows/ci.yml)

Kotlin Multiplatform port of [commonmark-java](https://github.com/commonmark/commonmark-java) for parsing and rendering CommonMark documents.

The repository contains the core `kotmark` module, shared test utilities, integration tests, and extensions for autolinks, footnotes, GFM alerts, strikethrough, tables, heading anchors, image attributes, insertions, task list items, and YAML front matter.

## Development

Run the JVM test suite with the project-local Kotlin Toolchain wrapper:

```shell
./kotlin test --platform jvm
```
