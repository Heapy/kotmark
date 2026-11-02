package io.heapy.kotmark.robustness

import kotlin.random.Random

object MarkdownInputs {
    val seeds = listOf(
        "", "\u0000\uD800\uDC00\uFFFF", "a\r\nb\rc\n", "\t>\t* item\n",
        "[ref]\n\n[ref]: /url \"title\"\n", "![alt](/img){width=12 height=8}",
        "**bold** _italic_ ~~strike~~ ++insert++ ==highlight== \$x$",
        "```kotlin\nval x = 1\n```\n", "<div>\n<!-- comment -->\n</div>\n",
        "- [x] task\n- [ ] todo\n", "| a | b |\n| :- | -: |\n| c | d |\n",
        "> [!NOTE]\n> alert\n", "---\ntitle: sample\n---\n# heading\n",
        "note[^note]\n\n[^note]: definition\n", "$$\nx < y\n$$\n",
        "https://example.com/a_(b) user@example.com www.example.com",
        "&amp; &#0; &#x10FFFF; \\*escaped\\*", "# same\n\n# same\n",
        "[ref] [^note]\n", "[[[[[[[[(((((((********________````````",
    )
    private val tokens = seeds + listOf("\n", "\r\n", "\t", " ", "[", "]", "(", ")", "*", "_", "`", "\\", "<", ">", "&", "|", "$", "=", "+", "~")

    fun arbitrary(random: Random): String = buildString {
        repeat(random.nextInt(0, 40)) {
            when (random.nextInt(4)) {
                0 -> append(random.nextInt(0x10000).toChar()) // Includes lone UTF-16 surrogates.
                else -> append(tokens.random(random))
            }
        }
    }

    fun mutate(random: Random, source: String): String {
        var result = source
        repeat(random.nextInt(1, 6)) {
            val start = random.nextInt(result.length + 1)
            val end = random.nextInt(start, result.length + 1)
            result = when (random.nextInt(4)) {
                0 -> result.removeRange(start, end)
                1 -> result.take(start) + tokens.random(random) + result.substring(start)
                2 -> result.take(start) + result.substring(start, end).repeat(2) + result.substring(end)
                else -> result.take(start) + random.nextInt(0x10000).toChar() + result.substring(end)
            }.take(2048)
        }
        return result
    }

    /** A bounded grammar with independent block boundaries and unambiguous inline contents. */
    fun structured(random: Random): String = buildString {
        fun word() = buildString { repeat(random.nextInt(1, 25)) { append(('a'..'z').random(random)) } }
        repeat(random.nextInt(1, 15)) {
            val text = word()
            append(when (random.nextInt(8)) {
                0 -> "${"#".repeat(random.nextInt(1, 7))} $text"
                1 -> "$text **${word()}** _${word()}_ `${word()}`"
                2 -> "[${word()}](https://example.com/$text)"
                3 -> "> $text\n>\n> ${word()}"
                4 -> "- $text\n- ${word()}"
                5 -> "1. $text\n2. ${word()}"
                6 -> "```text\n$text\n```"
                else -> "$text\n${word()}"
            })
            append("\n\n")
        }
    }
}
