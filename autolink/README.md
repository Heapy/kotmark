# Autolink extractor

Extract URLs and email addresses from plain text with the standalone `autolink` module.
For Markdown, use `kotmark-ext-autolink` with the parser and renderer.

```kotlin
import io.heapy.kotmark.autolink.LinkType
import io.heapy.kotmark.autolink.linkExtractor

val extractor = linkExtractor {
    linkTypes = setOf(LinkType.URL, LinkType.EMAIL)
    emailDomainMustHaveDot = true
}
val links = extractor.extractLinks("Visit https://example.com")
```

## Behavior and API

The extractor API is in `io.heapy.kotmark.autolink`; upstream package names are not
retained. CommonMark APIs and extensions are under `io.heapy.kotmark`.

UTF-16 begin/end indices retain the upstream inclusive/exclusive semantics. Extraction is lazy, iterables are reusable, and iterators keep independent state. Configuration copies the caller’s link-type set; an empty set disables extraction. The default email domain must contain a dot. Character classification uses the explicit upstream ASCII and UTF-16 rules, independent of backend Unicode tables.

There is no GFM mode. `kotmark-ext-autolink` uses the same extraction rules
when recognizing bare links in Markdown.

## GFM differences

The extractor follows autolink-java rules. These differ from GFM in three
examples from its Autolinks specification section:

| Example within section | Difference |
|---|---|
| 5 | Upstream stops at an unmatched closing parenthesis even when more URL text follows; GFM only checks excess closing parentheses at the end. |
| 6 | Upstream retains `&hl` inside the link and leaves the final semicolon outside; GFM excludes the whole `&hl;` suffix. |
| 11 | Upstream links the email prefix before a trailing `-` or `_`; GFM leaves those addresses unlinked. |

Only lowercase `www` is recognized.

[Upstream revisions and licenses](../UPSTREAM.md) ·
[Upstream test mapping](../docs/autolink-testing.md)
