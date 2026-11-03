---
title: Credits & notices
nav: Credits & notices
kicker: Project / lineage
description: Upstream projects, specification corpora, and third-party license notices for kotmark.
lede: Kotmark ports commonmark-java to Kotlin Multiplatform and includes a port of autolink-java. Preserve the notices of third-party material when redistributing it.
cards: []
---

## Ported sources

| Source | Used in | License |
| --- | --- | --- |
| [commonmark-java](https://github.com/commonmark/commonmark-java) | Core parser, renderers, extensions, and tests | BSD-2-Clause |
| [autolink-java](https://github.com/robinst/autolink-java) | `autolink` module and tests | MIT |

## Specification corpora

The CommonMark 0.31.2, commonmark-java, and GFM corpora are retained as separate fixtures. See the [project provenance record](credits.html#provenance) for pinned revisions and checksums.

## References

- [JetBrains Markdown](https://github.com/JetBrains/markdown) was used to check math and highlight grammars; no parser code from it is bundled.
- [kotlinx.html](https://github.com/Kotlin/kotlinx.html) provides the optional HTML DSL renderer.

## Provenance

The revisions and corpus checksums are recorded in `UPSTREAM.md`. The notices below are included verbatim from that record.

## License notices {#upstream-notices}

```text upstream="MIT notice for autolink"
```

```text upstream="BSD notice for commonmark-java"
```

```text upstream="BSD notice for test utility resources"
```

```text upstream="BSD notice for cmark and commonmark.js regression fixtures"
```

```text upstream="Unicode notice"
```