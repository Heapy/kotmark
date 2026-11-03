# Migrating to Kotmark

Kotmark is a Kotlin Multiplatform port of commonmark-java. Packages use
`io.heapy.kotmark`; it is not a Java binary-compatible replacement.
[UPSTREAM.md](../UPSTREAM.md) records the source revisions.

## Configuration factories

Top-level configuration factories replace the builder API and is a source and binary breaking change.
Replace `Parser.builder().includeSourceSpans(value).build()` with
`parser { includeSourceSpans = value }`, and import the factory function.
Scalar options are properties. Registration functions such as `extensions(...)`,
`nodeRendererFactory(...)`, and `postProcessor(...)` return `Unit` and run in order.
Extension hooks receive `Parser.Configuration`, `HtmlRenderer.Configuration`,
`MarkdownRenderer.Configuration`, or `TextContentRenderer.Configuration`.

For alerts, use `allowedTypes`, `customTitlesAllowed`, and `nestedAlertsAllowed`
properties in place of `setAllowedTypes`, `allowCustomTitles`, and `allowNestedAlerts`.
Use `linkTypes = setOf(...)` for autolinks, and
`lineBreakRendering = LineBreakRendering.STRIP` instead of `stripNewlines(true)`.
Replace `matcher.newBuilder()` with `asciiMatcher(matcher) { … }`.
Existing no-argument extension `create()` factories remain available.

See [configuration](configuration.md#factories) for the class-to-factory mapping.

## Block continuation

`BlockContinue` can no longer be subclassed by extensions, and `BlockContinueImpl`
has been removed. This breaks source and binary compatibility for code that
subclassed `BlockContinue` or referenced that implementation class. Such extensions
must migrate and recompile: return a public outcome, and replace implementation
casts and sentinel checks with `when` branches for `AtIndex`, `AtColumn`,
`Finished`, and (when the input is nullable) `null`. Each position outcome carries
only its own coordinate; `Finished` has no coordinate or Boolean flag.

See [custom block continuation](custom-blocks.md) for the public outcomes and an example.

## Standalone autolink API

Java getters become Kotlin properties (with equivalent JVM getters); nullable inputs are excluded by Kotlin types. The `linkExtractor { … }` configuration DSL retains the upstream options as `linkTypes` and `emailDomainMustHaveDot` properties; it replaces the upstream builder API. Kotlin read-only `Iterator` omits `remove`; the generated JVM implementation still throws `UnsupportedOperationException`. Internal implementation classes are Kotlin `internal`. Deprecated `Autolink.renderLinks` and `LinkRenderer` are retained. Java module descriptors and package-info documentation are not KMP runtime components.
