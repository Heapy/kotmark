# HTML renderer traversal experiments

## Approaches (abridged diffs)

**Stack:** `ArrayDeque<Frame>`, with callbacks to close tags.

```diff
- renderChildren(node)
- writer.tag("/strong")
+ renderChildren(node) { writer.tag("/strong") }
```

**Compact stack:** `IntArray` + `Array<Any?>`, a pointer to the next node, and built-in handlers without closures.

```diff
- context.renderChildren(node) { html.tag("/strong") }
+ context.renderCoreChildren(node, "/strong")
```

**DRF + when:** `DeepRecursiveFunction`, suspending handlers, and dispatch by node type.

```diff
- fun HtmlNodeRendererContext.renderChildren(parent: Node)
+ suspend fun DeepRecursiveScope<Node, Unit>.renderChildren(parent: Node)
- render(child)
+ callRecursive(child)
```

**DRF + visitor:** `SuspendAbstractVisitor` and a suspending version of `Node.accept`.

```diff
- when (node) { /* dispatch by type */ }
+ with(node) { accept(this@PrototypeCoreHtmlNodeRenderer) }
- private suspend fun DeepRecursiveScope<Node, Unit>.visit(heading: Heading)
+ override suspend fun DeepRecursiveScope<Node, Unit>.visit(heading: Heading)
```

## Measurements

M4 Max; JDK 25.0.4; Kotlin 2.4.21. Three JVM runs per approach: 5 × 300 ms warmup, 7 × 300 ms measurement; baseline and prototype alternate. The AST is prepared in advance. These are not JMH benchmarks.

Thread CPU time relative to the recursive baseline = **1.00×**; lower is better. Values are the median of the medians of paired ratios. Stack and DRF approaches were measured in separate runs; small differences may be noise.

| AST | Stack | Compact stack | DRF + when | DRF + visitor |
|---|---:|---:|---:|---:|
| Mixed Markdown | 1.25× | 1.06× | 1.49× | 1.47× |
| Same document ×100 | 1.45× | 1.14× | 1.59× | 1.59× |
| 10,000 sibling Text nodes | 0.84× | 0.99× | 0.64× | 0.63× |
| 128 nested StrongEmphasis nodes | 0.84× | 0.55× | 0.73× | 0.70× |
| 128 nested custom Emphasis nodes | 1.05× | 0.63× | 1.14× | 1.10× |

The first two scenarios use generated Markdown; the others use synthetic ASTs.

Allocations for the document ×100 scenario, KiB/render: **baseline 576; stack 662; compact stack 572; both DRF approaches 827**.

All prototypes matched the baseline on CommonMark and handled a nesting depth of 100,000; the baseline threw `StackOverflowError` at that depth.

Validation scope: core HTML/JVM. Extensions and lifecycle hooks were not ported; the parser was unchanged.
