# Autolink upstream test mapping

This reference records how the pinned upstream tests map to the Kotlin suites.
For extraction behavior, see the [autolink guide](../autolink/README.md).

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

All nine upstream CommonMark extension methods are retained under `UpstreamAutolinkTest`: `oneTextNode`, `textNodeAndOthers`, `tricky`, `emailUsesMailto`, `emailWithTldNotLinked`, `dontLinkTextWithinLinks`, `wwwLinks`, `noWwwLinks`, and `sourceSpans`. Their original strings and all source-span assertions are preserved. Eight existing Kotmark regression methods remain; three previous non-upstream expectations were corrected against the unmodified Java reference to match the pinned upstream behavior.

## Running the suites

```shell
./kotlin test --platform jvm -m autolink -m kotmark-ext-autolink
```

See [development](development.md#conformance-and-platform-tests) for other targets.
