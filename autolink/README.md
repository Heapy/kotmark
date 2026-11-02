# Autolink port

Upstream revisions and license notices are recorded in [UPSTREAM.md](../UPSTREAM.md).

## API adaptation

The extractor API is in `io.heapy.kotmark.autolink`; upstream package names are not
retained. CommonMark APIs and extensions are under `io.heapy.kotmark`.

UTF-16 begin/end indices retain the upstream inclusive/exclusive semantics. Extraction is lazy, iterables are reusable, and iterators keep independent state. Configuration copies the caller’s link-type set; an empty set disables extraction. The default email domain must contain a dot. Character classification uses the explicit upstream ASCII and UTF-16 rules, independent of backend Unicode tables.

Java getters become Kotlin properties (with equivalent JVM getters); nullable inputs are excluded by Kotlin types. Builders retain the upstream options and `builder()` is exposed with `@JvmStatic`. Kotlin read-only `Iterator` omits `remove`; the generated JVM implementation still throws `UnsupportedOperationException`. Internal implementation classes are Kotlin `internal`. Deprecated `Autolink.renderLinks` and `LinkRenderer` are retained. Java module descriptors and package-info documentation are not KMP runtime components.

There is no GFM mode. `kotmark-ext-autolink` delegates extraction directly to this module, retaining the upstream AST handling, type configuration, source-span splitting, nested-link suppression, and href prefixes.

## Upstream test mapping

The 48 upstream test methods expand to **97 test invocations** after class parameters: 95 common tests and two JVM-only Java iterator-removal tests. No upstream test is disabled. `AutolinkBenchmark` contains JMH benchmarks, not JUnit tests, and is not part of this count.

Each original method maps to the same-named Kotlin method, with `ConfigurationN` appended for parameterized classes. The following table enumerates every method.

| Upstream class | Method | Kotlin invocations |
|---|---|---|
| `AutolinkUrlTest` | `notLinked` | `AutolinkUrlTest.notLinkedConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `schemes` | `AutolinkUrlTest.schemesConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `authority` | `AutolinkUrlTest.authorityConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `linking` | `AutolinkUrlTest.linkingConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `schemeSeparatedByNonAlphanumeric` | `AutolinkUrlTest.schemeSeparatedByNonAlphanumericConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `spaceCharactersStopUrl` | `AutolinkUrlTest.spaceCharactersStopUrlConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `illegalCharactersStopUrl` | `AutolinkUrlTest.illegalCharactersStopUrlConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `delimiterAtEnd` | `AutolinkUrlTest.delimiterAtEndConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `matchingPunctuation` | `AutolinkUrlTest.matchingPunctuationConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `matchingPunctuationTricky` | `AutolinkUrlTest.matchingPunctuationTrickyConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `quotes` | `AutolinkUrlTest.quotesConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `html` | `AutolinkUrlTest.htmlConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `css` | `AutolinkUrlTest.cssConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `slash` | `AutolinkUrlTest.slashConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `multiple` | `AutolinkUrlTest.multipleConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `international` | `AutolinkUrlTest.internationalConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `unicodeWhitespace` | `AutolinkUrlTest.unicodeWhitespaceConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `replyLevel` | `AutolinkUrlTest.replyLevelConfiguration1`–`Configuration2` |
| `AutolinkUrlTest` | `linkToString` | `AutolinkUrlTest.linkToStringConfiguration1`–`Configuration2` |
| `AutolinkWwwTest` | `notLinked` | `AutolinkWwwTest.notLinkedConfiguration1`–`Configuration2` |
| `AutolinkWwwTest` | `linked` | `AutolinkWwwTest.linkedConfiguration1`–`Configuration2` |
| `AutolinkWwwTest` | `html` | `AutolinkWwwTest.htmlConfiguration1`–`Configuration2` |
| `AutolinkWwwTest` | `multiple` | `AutolinkWwwTest.multipleConfiguration1`–`Configuration2` |
| `AutolinkWwwTest` | `international` | `AutolinkWwwTest.internationalConfiguration1`–`Configuration2` |
| `AutolinkWwwTest` | `replyLevel` | `AutolinkWwwTest.replyLevelConfiguration1`–`Configuration2` |
| `AutolinkEmailTest` | `notLinked` | `AutolinkEmailTest.notLinkedConfiguration1`–`Configuration3` |
| `AutolinkEmailTest` | `simple` | `AutolinkEmailTest.simpleConfiguration1`–`Configuration3` |
| `AutolinkEmailTest` | `allowedText` | `AutolinkEmailTest.allowedTextConfiguration1`–`Configuration3` |
| `AutolinkEmailTest` | `spaceSeparation` | `AutolinkEmailTest.spaceSeparationConfiguration1`–`Configuration3` |
| `AutolinkEmailTest` | `specialSeparation` | `AutolinkEmailTest.specialSeparationConfiguration1`–`Configuration3` |
| `AutolinkEmailTest` | `dots` | `AutolinkEmailTest.dotsConfiguration1`–`Configuration3` |
| `AutolinkEmailTest` | `domainWithoutDot` | `AutolinkEmailTest.domainWithoutDotConfiguration1`–`Configuration3` |
| `AutolinkEmailTest` | `dashes` | `AutolinkEmailTest.dashesConfiguration1`–`Configuration3` |
| `AutolinkEmailTest` | `multiple` | `AutolinkEmailTest.multipleConfiguration1`–`Configuration3` |
| `AutolinkEmailTest` | `international` | `AutolinkEmailTest.internationalConfiguration1`–`Configuration3` |
| `AutolinkEmailTest` | `triggerOverlap` | `AutolinkEmailTest.triggerOverlapConfiguration1`–`Configuration3` |
| `AutolinkEmailTest` | `replyLevel` | `AutolinkEmailTest.replyLevelConfiguration1`–`Configuration3` |
| `LinkIterableTest` | `iteratorIsNew` | `LinkIterableTest.iteratorIsNew` |
| `LinkIterableTest` | `hasNextOnlyAdvancesOnce` | `LinkIterableTest.hasNextOnlyAdvancesOnce` |
| `LinkIterableTest` | `nextThrowsNoSuchElementException` | `LinkIterableTest.nextThrowsNoSuchElementException` |
| `LinkIterableTest` | `removeUnsupported` | `IteratorRemoveTest.linkIterableRemoveUnsupported` (JVM) |
| `SpanIterableTest` | `iteratorIsNew` | `SpanIterableTest.iteratorIsNew` |
| `SpanIterableTest` | `hasNextOnlyAdvancesOnce` | `SpanIterableTest.hasNextOnlyAdvancesOnce` |
| `SpanIterableTest` | `spanAndLinkSequences` | `SpanIterableTest.spanAndLinkSequences` |
| `SpanIterableTest` | `nextThrowsNoSuchElementException` | `SpanIterableTest.nextThrowsNoSuchElementException` |
| `SpanIterableTest` | `removeUnsupported` | `IteratorRemoveTest.spanIterableRemoveUnsupported` (JVM) |
| `UsageExampleTest` | `linkify` | `UsageExampleTest.linkify` |
| `UsageExampleTest` | `extractLinks` | `UsageExampleTest.extractLinks` |

Configuration 1 restricts extraction to the class’s link type. Configuration 2 enables all types. For email, both require a domain dot; configuration 3 enables all types and allows a dotless domain. All original inputs and expected values are preserved, including duplicate assertions and each Unicode-whitespace case.

`AutolinkTestCase` checks both link-only and full-span extraction. Upstream accidentally calls `extractSpans` in both rendering helpers; the Kotlin helper additionally exercises `extractLinks` without changing expectations. Four additional `LinkExtractorTest` methods cover empty input/disabled types, configuration snapshots and independent iterators, supplementary/unpaired surrogates with exact UTF-16 bounds, and uppercase WWW rejection. These are outside the 97 upstream invocations.

All nine upstream CommonMark extension methods are retained under `UpstreamAutolinkTest`: `oneTextNode`, `textNodeAndOthers`, `tricky`, `emailUsesMailto`, `emailWithTldNotLinked`, `dontLinkTextWithinLinks`, `wwwLinks`, `noWwwLinks`, and `sourceSpans`. Their original strings and all source-span assertions are preserved. Eight existing Kotmark regression methods remain; three previous non-upstream expectations were corrected against the unmodified Java reference as requested.

## GFM differences

The 11 examples from the pinned `gfm-spec.txt` Autolinks section are embedded in common test sources, with original GFM HTML and separate Java reference HTML. The GFM corpus is attributed to GitHub / John MacFarlane under CC-BY-SA 4.0 in the fixture header. `AutolinkSpecTest` verifies upstream behavior on all 11 examples, not full GFM conformance. The differences are:

| Example within section | Difference |
|---|---|
| 5 | Upstream stops at an unmatched closing parenthesis even when more URL text follows; GFM only checks excess closing parentheses at the end. |
| 6 | Upstream retains `&hl` inside the link and leaves the final semicolon outside; GFM excludes the whole `&hl;` suffix. |
| 11 | Upstream links the email prefix before a trailing `-` or `_`; GFM leaves those addresses unlinked. |

Kotmark’s previous uppercase `WWW` expectation also differed from upstream, which recognizes lowercase `www` only. These are deliberate corrections to match the requested upstream behavior, not altered upstream assertions.

## Reproduction

The autolink and extension suites use committed Kotlin test data. Shared corpus files for the core, other extensions, and integration suites are embedded as common Kotlin text in `test-fixtures`, with [exact revisions, hashes and licenses](../UPSTREAM.md). No upstream checkout or fixture-download script is needed:

```shell
./kotlin test --platform jvm
./kotlin test -m autolink -m kotmark-ext-autolink --platform macosArm64
node scripts/test-js.mjs
```

Use `linuxX64` instead of `macosArm64` on a Linux host. CI runs the same bundled data; the Node runner explicitly includes `autolink`. During the port, all 11 GFM inputs and original expectations were compared with the pinned specification, and their separate reference HTML was verified by compiling the unmodified Java upstream sources. The resulting data is retained in `GfmAutolinkExamples.kt`; that one-time verification does not add a build-time source download or helper-script requirement.

## Verification results

Validation environment: macOS ARM64, Kotlin Toolchain 0.13.0, Kotlin 2.4.21. The wrapper and general CI matrix were updated by parallel work; they are not changes owned by this port.

- One-time Java reference verification: compiled fresh, pinned, unmodified upstream sources and verified all 11 original inputs/GFM expectations and reference outputs. Differences are exactly examples 5, 6, 11. The temporary verification and fixture-fetch scripts were removed after bundling the test data.
- `node scripts/test-js.mjs`: 318 passed, 0 failed, 0 skipped across the project, including all 99 common autolink tests and 19 extension tests; exit 0.
- Library compilation: all 18 targets present at the time of the check for both `autolink` and `kotmark-ext-autolink`; exit 0. Parallel platform maintenance subsequently removed `iosX64` and `watchosArm32` from these modules, leaving 16 targets, all covered by that successful check. This is compilation evidence, not runtime verification of every target.
- `./kotlin build -m autolink -m kotmark-ext-autolink`: attempted broader compilation and test-executable linking, exited 1 at `:autolink:linkWatchosArm32TestDebug`: Xcode reports `ld: -arch armv7k is no longer supported`. The legacy target’s library compiled; the later removal from the module matrix belongs to parallel platform maintenance. No watchOS ARM32 runtime or executable-linking claim is made.
- `./kotlin test --platform jvm --platform macosArm64`: exit 0; **334 JVM tests** and **318 macOS Native tests** passed, 0 failures/skips. The JVM run includes all 97 upstream extractor invocations, 4 added extractor tests, 19 extension methods (including the 9 upstream methods and 11 GFM data cases), and 4 integration methods. Native executes the 95 common upstream invocations plus 4 added extractor tests; the Java-only `remove` checks have no common Kotlin API equivalent.
- After bundling the shared fixtures, `./kotlin test --platform jvm` passed **336 tests**, zero failures/skips, in an isolated source copy without `projects/` or existing build outputs. The two additional tests verify fixture hashes and example counts. Test data no longer requires either helper script or an upstream checkout.
- `git diff --check`: passed.

To repeat the compilation-only check for the current 16-target matrix:

```sh
set --
for module in autolink kotmark-ext-autolink; do
    for suffix in Jvm AndroidDebug Js WasmJs WasmWasi LinuxX64Debug LinuxArm64Debug MacosArm64Debug TvosArm64Debug TvosSimulatorArm64Debug IosArm64Debug IosSimulatorArm64Debug WatchosArm64Debug WatchosDeviceArm64Debug WatchosSimulatorArm64Debug MingwX64Debug; do
        set -- "$@" ":$module:compile$suffix"
    done
done
./kotlin task "$@"
```

CI is configured to include the new module, but no remote CI run was triggered in this session. Successful local runtime checks do not establish equivalence on every possible input or runtime coverage on other platforms.
