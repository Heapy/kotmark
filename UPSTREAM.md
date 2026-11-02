# Upstream sources and notices

Kotmark ports commonmark-java to Kotlin Multiplatform and includes an independent
autolink-java port. The revisions below identify the sources used, not the latest
upstream releases. Original Kotmark code and modifications are licensed under the
[Apache License 2.0](LICENSE). That license does not replace the licenses or
notices of the third-party material listed below.

When distributing derived code or data, retain the applicable third-party notices.
They may be supplied in this document or incorporated into the distribution's
own license documentation; the filename and unrelated notices need not be retained.

## Code

| Source | Pinned revision | Local scope | License |
| --- | --- | --- | --- |
| [commonmark-java](https://github.com/commonmark/commonmark-java) | [9477a93b6b0965efc54c55bd40ad88fbbe25bc6f](https://github.com/commonmark/commonmark-java/tree/9477a93b6b0965efc54c55bd40ad88fbbe25bc6f), 0.29.1-SNAPSHOT | `kotmark`, `kotmark-ext-*`, test utilities and ported tests | BSD-2-Clause, Robin Stocker; test utility resources also retain Atlassian's notice |
| [autolink-java](https://github.com/robinst/autolink-java) | [8b07a6911fd62871042c5b63412d3ac3489a0531](https://github.com/robinst/autolink-java/tree/8b07a6911fd62871042c5b63412d3ac3489a0531), tag `autolink-0.12.0` | `autolink` production code and tests | MIT, Robin Stocker |

The commonmark-java revision is dated 2026-06-20, subject `README: Bump version`,
described by Git as `commonmark-parent-0.29.0-2-g9477a93b`.
The autolink annotated tag object is `b5fb809bf0c62e19e5eedbf40f5d00d1eb349a50`.

Java sources and tests are translated and adapted to Kotlin Multiplatform.
Autolink preserves upstream behavior without an additional GFM mode; API
adaptations, test coverage and known GFM differences are documented in
[autolink/README.md](autolink/README.md).

## Test fixtures

### JetBrains Markdown reference

The math and highlight extension design was checked against
[JetBrains Markdown at a8564bfb1123b5edd0fc2569d65e3efe11d9e802](https://github.com/JetBrains/markdown/tree/a8564bfb1123b5edd0fc2569d65e3efe11d9e802)
in the local `projects/markdown` checkout (Apache-2.0). Reference files are
`MathParser.kt`, `HighlightDelimiterParser.kt`, and
`src/fileBasedTest/resources/data/parser/{math,highlight}.md`.
The new tests include adapted syntax examples, including the escaped dollar in
`\sqrt{\$4}`. No JetBrains parser code or full fixture files are bundled.
Kotmark's math grammar differs intentionally; see
[the delimiter contract](kotmark-ext-math/README.md).

### Bundled CommonMark corpora

The six files below are bundled byte for byte under
`test-fixtures/resources/upstream/`.
[SHA256SUMS](test-fixtures/resources/upstream/SHA256SUMS) pins their bytes.
`scripts/generate-test-fixtures.main.kts` converts their complete UTF-8 text to
committed common Kotlin sources in `test-fixtures/src/testfixtures/generated/`,
preserving every character, line ending and notice. These generated files have
the same respective licenses as their source corpora.

All platforms read the same embedded text without filesystem or network access.
JVM integrity tests compare its encoded bytes to every original resource and
SHA-256; common integrity tests verify all six example counts. Ordinary test runs
do not require generation or upstream checkouts. This module is test infrastructure
and is not a dependency of the production parsing and rendering libraries.

| Bundled file | Source at pinned revision | Examples | License |
| --- | --- | --- | --- |
| `commonmark-java/spec.txt` | commonmark-java `commonmark-test-util/src/main/resources/spec.txt` | 652 | CC-BY-SA-4.0 |
| `commonmark-java/gfm-spec.txt` | commonmark-java `commonmark-test-util/src/main/resources/gfm-spec.txt` | 672 | CC-BY-SA-4.0 |
| `commonmark-java/cmark-regression.txt` | commonmark-java `commonmark-test-util/src/main/resources/cmark-regression.txt` | 15 | BSD-2-Clause, John MacFarlane |
| `commonmark-java/commonmark.js-regression.txt` | commonmark-java `commonmark-test-util/src/main/resources/commonmark.js-regression.txt` | 20 | BSD-2-Clause, John MacFarlane |
| `commonmark-java/alerts-spec.txt` | commonmark-java `commonmark-ext-gfm-alerts/src/test/resources/alerts-spec.txt` | 33 | BSD-2-Clause, Robin Stocker |
| `commonmark-spec/spec.txt` | [commonmark-spec 3da939428d80f146f270cd1765e4ba462e96bb1b](https://github.com/commonmark/commonmark-spec/tree/3da939428d80f146f270cd1765e4ba462e96bb1b), root `spec.txt` | 655 | CC-BY-SA-4.0 |

CommonMark specification text is by **John MacFarlane**, with the upstream notice
**Copyright (C) 2014-16 John MacFarlane**. The **GitHub Flavored Markdown Spec**
is by **GitHub**, based on CommonMark. Both are licensed under
[Creative Commons Attribution-ShareAlike 4.0 International](https://creativecommons.org/licenses/by-sa/4.0/)
([legal text](https://creativecommons.org/licenses/by-sa/4.0/legalcode)).
The original metadata and license notices remain in the unmodified corpus files.

The 11 GFM autolink examples in
`kotmark-ext-autolink/test/ext/autolink/GfmAutolinkExamples.kt`
are extracted from the pinned GFM corpus under the same CC-BY-SA-4.0 license.
Their original inputs and GFM outputs are retained; separate commonmark-java
reference outputs and Kotlin test scaffolding have been added.

The commonmark-java resource README attributes the copied fixtures to
[commonmark-spec](https://github.com/commonmark/commonmark-spec/blob/master/spec.txt),
[cmark](https://github.com/commonmark/cmark/blob/master/test/regression.txt), and
[commonmark.js](https://github.com/commonmark/commonmark.js/blob/master/test/regression.txt),
under the licenses stated in those repositories.
The John MacFarlane BSD notice below comes from
[cmark COPYING at 5290e3edfade70e7c8c251a05b3239630a1dad81](https://github.com/commonmark/cmark/blob/5290e3edfade70e7c8c251a05b3239630a1dad81/COPYING)
and also applies to the commonmark.js regression fixture, whose notice was checked
against [commonmark.js LICENSE at 898886961b49c1df1175666d03c3ac66c9cc9040](https://github.com/commonmark/commonmark.js/blob/898886961b49c1df1175666d03c3ac66c9cc9040/LICENSE).
Those two revisions identify license snapshots, not the earlier fixture import
commits; the fixture versions are pinned by commonmark-java and SHA-256 above.

When updating fixtures, record the new source revision, checksums and example
counts, and preserve attribution and license notices. Keep the two CommonMark
corpus versions distinct. Run `kotlinr scripts/generate-test-fixtures.main.kts`
after updating the originals, then run the JVM and common platform suites.

## Unicode data

The supplementary category ranges in
`kotmark/src/internal/util/UnicodeCategories.kt` are generated
from [UnicodeData.txt 18.0.0](https://www.unicode.org/Public/18.0.0/ucd/UnicodeData.txt).
Input SHA-256: `0736451de439ae7baf1425136617da495e09ee5afbe6e394374db7009ea08950`.
The applicable Unicode notice is retained below.

Regenerate from the project root with `kotlinr scripts/generate-unicode-categories.main.kts`.
Add `--check` to verify the committed table without changing files. Both modes
download the pinned input and verify its checksum before processing it.
This table covers code points above U+FFFF; BMP categories and the JVM/JS parser
helpers still use their platform's Unicode data.

## MIT notice for autolink

The MIT License (MIT)

Copyright (c) 2015 Robin Stocker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## BSD notice for commonmark-java

Copyright (c) 2015, Robin Stocker
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

## BSD notice for test utility resources

Copyright (c) 2015, Atlassian Pty Ltd
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

## BSD notice for cmark and commonmark.js regression fixtures

Copyright (c) 2014, John MacFarlane

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

## Unicode notice

UNICODE LICENSE V3

COPYRIGHT AND PERMISSION NOTICE

Copyright © 1991-2026 Unicode, Inc.

NOTICE TO USER: Carefully read the following legal agreement. BY
DOWNLOADING, INSTALLING, COPYING OR OTHERWISE USING DATA FILES, AND/OR
SOFTWARE, YOU UNEQUIVOCALLY ACCEPT, AND AGREE TO BE BOUND BY, ALL OF THE
TERMS AND CONDITIONS OF THIS AGREEMENT. IF YOU DO NOT AGREE, DO NOT
DOWNLOAD, INSTALL, COPY, DISTRIBUTE OR USE THE DATA FILES OR SOFTWARE.

Permission is hereby granted, free of charge, to any person obtaining a
copy of data files and any associated documentation (the "Data Files") or
software and any associated documentation (the "Software") to deal in the
Data Files or Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, and/or sell
copies of the Data Files or Software, and to permit persons to whom the
Data Files or Software are furnished to do so, provided that either (a)
this copyright and permission notice appear with all copies of the Data
Files or Software, or (b) this copyright and permission notice appear in
associated Documentation.

THE DATA FILES AND SOFTWARE ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF
THIRD PARTY RIGHTS.

IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS INCLUDED IN THIS NOTICE
BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR CONSEQUENTIAL DAMAGES,
OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THE DATA
FILES OR SOFTWARE.

Except as contained in this notice, the name of a copyright holder shall
not be used in advertising or otherwise to promote the sale, use or other
dealings in these Data Files or Software without prior written
authorization of the copyright holder.
