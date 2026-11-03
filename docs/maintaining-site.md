# Maintaining the documentation site

The landing page and shared visual assets are hand-written in `docs/site`; inner
pages are Markdown in `docs/pages`, rendered into ignored `build/site` by the JVM
`docs/site-builder` application. The builder reads `docs/pages/site.yml` with
kotaml, parses page Markdown with the current kotmark checkout, and copies the
static assets into the generated site. It makes no runtime CDN requests.

| Sources | Content |
| --- | --- |
| `docs/site/index.html` | Installation, CommonMark compatibility, custom blocks and renderers |
| `docs/pages/guides/*.md` | Configuration, URL resolution, block continuation, kotlinx.html, migration |
| `docs/pages/extensions/*.md` | Index and one usage/example page per extension |
| `docs/pages/contributing.md`, `credits.md` | Contributor checks, upstream lineage and license notices |
| `docs/pages/site.yml` | Ordered pages and navigation groups, decoded with kotaml |
| `docs/site/styles.css`, `site.js`, `favicon.svg` | Shared site presentation and progressive enhancements |

When adding a page, add its source path to `pages` and its navigation entry to
`site.yml`. The order in `pages` controls previous/next links. Write internal links
as `.md` so they also work on GitHub; the builder rewrites them to `.html` in the
published site. Never link to Markdown files from generated HTML.

Kotlin code comes from the JVM module `docs/examples`. Use a fenced block with
`snippet=path#region` to display a source region; the builder fails if the file or
region is missing. Fences with `example=<id>` show the Markdown input beside output
computed by the corresponding example function. Tests in `docs/examples` assert
those outputs. Fences with `upstream="section title"` include the exact section
body from `UPSTREAM.md`; the site check verifies it.

## Preview and validate

From the repository root, with `kotlinr` available:

```shell
./kotlin run --platform jvm -m site-builder
kotlinr docs/check.main.kts
kotlinr docs/serve.main.kts
```

The check walks generated pages and fails on duplicate IDs, missing image
descriptions, broken local links or cross-page anchors, Markdown links in output,
and snippets or notices that differ from their sources. The preview is served on
`127.0.0.1:8000` by the JDK's built-in file server. Pass a port or site directory
to change the defaults.

Compile and execute the site builder and examples with:

```shell
./kotlin test --platform jvm -m examples -m site-builder
```

## Build generated reference pages

The API and coverage indexes are generated from the reports that already exist
under `build/`. Generate the pages first, then assemble them with the reports:

```shell
for module in autolink kotmark kotmark-ext-* kotmark-renderer-kotlinx-html; do
  ./kotlin do dokkaHtml -m "$module" --build-dir build/documentation || exit
done
kotlinr plugins/kover/coverage.main.kts
./kotlin run --platform jvm -m site-builder
kotlinr docs/assemble.main.kts
kotlinr docs/check.main.kts --assembled build/pages
```

Dokka reads the real source-set model; Android-specific sources require the configured
Android SDK, and Native-specific sources need their SDKs (macOS with Xcode here).
The loop keeps documentation generation sequential to bound memory use. Kover reports
JVM line coverage per production library; it does not measure Native, Android,
or web coverage. The assembler fails if either report family is absent, and
copies the finished site to `build/pages`.
