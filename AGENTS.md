# Upstream reference

Kotmark is a Kotlin Multiplatform port of commonmark-java. Source repositories,
exact revisions and third-party notices are recorded in [UPSTREAM.md](UPSTREAM.md).
Update that document when incorporating a newer upstream revision.

The local upstream checkout is `projects/commonmark-java`; the additional
CommonMark specification reference is `projects/commonmark-spec`. The
`projects/` directory is ignored by Git, so these optional reference checkouts may
be absent in a fresh clone. Tests must use the bundled `test-fixtures` resources.
