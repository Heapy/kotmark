# Working in Kotmark

All Kotlin packages must use `io.heapy.kotmark` or a subpackage.
Source repositories, exact revisions, and third-party notices are recorded in
[UPSTREAM.md](UPSTREAM.md). Preserve notices when adapting upstream code or fixtures.

## Repository map

- `kotmark`: CommonMark parser, AST, and HTML/Markdown/text renderers.
- `kotmark-ext-*`: opt-in syntax extensions; keep dependencies scoped to the extension.
- `autolink`: standalone extractor used by `kotmark-ext-autolink`.
- `test-fixtures`: shared, embedded specification corpora; never a production dependency.
- `integration-test`: cross-module regressions, property tests, and fuzz invariants.
- `fuzz`, `benchmarks`, `benchmark-runners`: verification tools, outside production dependencies.
- `docs/examples`: compiled examples used by `docs/site`.
- `docs/site-builder`: JVM-only Markdown-to-HTML publisher for GitHub Pages.

## Build and verification

Restore locked build plugins before using the repository's `./kotlin` wrapper:

```shell
./ktc-plugins sync
./ktc-plugins verify
```

Plugin sources under ignored `plugins/` are downloaded dependencies. Change their
versions through the manifest and lock, not by editing the restored installation.
Production libraries use `library.module-template.yaml`; test and benchmark
modules use the common template. JVM artifacts and tests target Java 17; preserve
the shared test stack setting needed by deeply nested fixtures.

Choose checks for the change:

| Change | Verification |
| --- | --- |
| Parser or extension behavior | `./kotlin test --platform jvm -m <module>`; add a focused regression and run `integration-test` for interactions. |
| Shared/platform-specific behavior | Run the relevant Native or web suite; commands and host requirements are in [development](docs/development.md#conformance-and-platform-tests). |
| Public API | Run `node scripts/library-tools.mjs api-check <module>` before and after edits. Review compatibility before updating snapshots with `api-dump`. |
| Site or examples | `kotlinr docs/check.main.kts`; when Kotlin examples change, also run `./kotlin test --platform jvm -m examples`. |
| Pinned test corpora | Update originals, checksums, counts, and UPSTREAM.md; regenerate with `kotlinr scripts/generate-test-fixtures.main.kts`, then run fixture and affected spec suites. |

The full JVM suite is `./kotlin test --platform jvm`. Use [development](docs/development.md)
for fuzzing, coverage, benchmarks, and the CI target matrix. KLib API checks are
host-dependent; a local pass does not cover every target. Keep generated reports
under ignored `build/`; review committed API baselines and fixture sources explicitly.

## Behavioral contracts

- Configuration factories snapshot options and registrations. Registered objects
  themselves remain shared; do not imply that arbitrary user callbacks are thread-safe.
- The first registered renderer for a node type wins. Register custom overrides
  before the extension that supplies the default renderer.
- HTML destinations flow through resolve → optional sanitize → optional
  percent-encode → attribute escaping. Resolution must not mutate the AST.
- `BlockContinue.AtIndex` uses UTF-16 positions; `AtColumn` uses visual columns.
  `Finished` consumes the closing line; `null` lets the enclosing parser handle it.
- Keep both CommonMark corpora complete and distinct. Preserve upstream expected
  outputs; record intentional dialect differences explicitly rather than silently
  changing fixtures to fit an implementation.

## Documentation ownership

- `README.md`: project purpose, installation, one minimal example, platform families,
  high-level spec coverage, and links. Document `io.heapy.kotmark:kotmark:0.1.0`
  as the published release; local build metadata may still use a snapshot version.
- `docs/site` and `docs/pages`: a hand-written `index.html`, shared assets, and
  Markdown sources rendered to matching HTML pages by `docs/site-builder` with
  kotmark. `docs/pages/site.yml` is decoded with kotaml and orders navigation.
  Site links never point to `.md` files; `docs/check.main.kts` enforces this.
  Keep CI internals, report generation, and port history out of the user flow.
- `docs/README.md`: repository index pointing to the site and contributor docs.
- `docs/development.md`, `docs/maintaining-site.md`, and `docs/autolink-testing.md`:
  contributor procedures and test provenance.
- Module READMEs: module-specific usage, syntax, and integration contracts.
- `AGENTS.md`: actionable repository rules and pointers. Keep long tool explanations
  in development docs and third-party provenance in `UPSTREAM.md`.

When moving content, update inbound links. Do not add session transcripts, stale
local test totals, or implementation progress reports to user documentation.
Site snippets marked with `data-snippet` are loaded from their regions in
`docs/examples`; example outputs are computed from the same tested functions.
License text on `credits.html` (`data-upstream`) must match its `UPSTREAM.md` section.
