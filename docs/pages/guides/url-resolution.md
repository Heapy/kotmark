---
title: URL resolution
nav: URL resolution
kicker: Guide / HTML destinations
description: Resolve Markdown link and image destinations against a base URL or custom routes.
lede: URL resolution runs while rendering HTML, after parsing, and never mutates the AST.
cards: []
---

## Resolve against a base URL

The base URI resolver follows RFC 3986 path resolution and leaves destinations with a scheme unchanged.

```kotlin caption="Base URL resolution" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/guides/BaseUrl.kt#base-url
```

```example example=base-url
![Example](../images/example.png) [Top](#top)
```

## Route links in your application

Implement `UrlResolver` to map Markdown destinations to application routes or to use another host for images.

```kotlin caption="Custom routing" snippet=docs/examples/src/io/heapy/kotmark/docs/examples/guides/UrlRouting.kt#url-routing
```

```example example=url-routing
[Guide](guide.md) ![Logo](logo.png)
```

## Processing order

Core links, reference links, autolinks, and images go through resolve, optional sanitize, optional percent-encode, then HTML attribute escaping.

- Raw HTML is not affected.
- A fragment-only destination remains local by default.
- With `urlResolver = null`, rendering is unchanged.
