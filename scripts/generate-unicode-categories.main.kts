#!/usr/bin/env kotlinr

// Regenerate supplementary categories from the checksum-pinned Unicode Character Database.
// Run from the project root: kotlinr scripts/generate-unicode-categories.main.kts [--check]
import java.net.URI
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.readText
import kotlin.io.path.writeText

require(args.isEmpty() || args.contentEquals(arrayOf("--check"))) {
    "Usage: generate-unicode-categories.main.kts [--check] (from the project root)"
}
val checkOnly = args.isNotEmpty()
val version = "18.0.0"
val url = "https://www.unicode.org/Public/$version/ucd/UnicodeData.txt"
val sha256 = "0736451de439ae7baf1425136617da495e09ee5afbe6e394374db7009ea08950"
val path = Path.of("kotmark/src/internal/util/UnicodeCategories.kt")
val source = path.readText()
val marker = "    // Generated from Unicode "
check(source.indexOf(marker) >= 0 && source.indexOf(marker) == source.lastIndexOf(marker)) {
    "Expected exactly one generated table marker in $path"
}

// Match CharCategory / java.lang.Character values; value 17 is intentionally unused.
val categories = mapOf(
    "Cn" to 0, "Lu" to 1, "Ll" to 2, "Lt" to 3, "Lm" to 4, "Lo" to 5,
    "Mn" to 6, "Me" to 7, "Mc" to 8, "Nd" to 9, "Nl" to 10, "No" to 11,
    "Zs" to 12, "Zl" to 13, "Zp" to 14, "Cc" to 15, "Cf" to 16,
    "Co" to 18, "Cs" to 19, "Pd" to 20, "Ps" to 21, "Pe" to 22, "Pc" to 23,
    "Po" to 24, "Sm" to 25, "Sc" to 26, "Sk" to 27, "So" to 28, "Pi" to 29, "Pf" to 30,
)

val connection = URI(url).toURL().openConnection().apply {
    connectTimeout = 30_000
    readTimeout = 30_000
}
val bytes = connection.getInputStream().use { it.readBytes() }
check(MessageDigest.getInstance("SHA-256").digest(bytes).toHexString() == sha256) {
    "Unexpected UnicodeData.txt checksum for Unicode $version"
}

data class CategoryRange(val start: Int, val end: Int, val category: Int)

val ranges = mutableListOf<CategoryRange>()
var pending: CategoryRange? = null
for (line in bytes.decodeToString(throwOnInvalidSequence = true).lineSequence().filter { it.isNotBlank() }) {
    val (pointText, name, categoryName) = line.split(';')
    val point = pointText.toInt(16)
    if (point < 0x10000) continue
    check(point <= 0x10FFFF) { "Invalid code point: $pointText" }
    val category = categories.getValue(categoryName)
    if (name.endsWith(", First>")) {
        check(pending == null) { "Nested range start: $pointText" }
        pending = CategoryRange(point, point, category)
        continue
    }
    val start = if (name.endsWith(", Last>")) {
        val first = checkNotNull(pending) { "Range end without a start: $pointText" }
        check(first.category == category && first.start <= point) { "Invalid range: $pointText" }
        first.start
    } else {
        check(pending == null) { "Missing range end before $pointText" }
        point
    }
    val previous = ranges.lastOrNull()
    check(previous == null || previous.end < start) { "Overlapping or unsorted range: $pointText" }
    if (previous != null && previous.end + 1 == start && previous.category == category) {
        ranges[ranges.lastIndex] = previous.copy(end = point)
    } else {
        ranges.add(CategoryRange(start, point, category))
    }
    pending = null
}
check(pending == null && ranges.isNotEmpty()) { "Incomplete Unicode category data" }

fun hex(point: Int): String = "0x" + point.toString(16).uppercase()

val generated = buildString {
    append(source.substringBefore(marker))
    appendLine("    // Generated from Unicode $version by scripts/generate-unicode-categories.main.kts.")
    appendLine("    // Unicode data license: UPSTREAM.md.")
    appendLine("    // Each triple is an inclusive start, inclusive end, and CharCategory value.")
    appendLine("    private val SUPPLEMENTARY_RANGES = intArrayOf(")
    for (row in ranges.chunked(3)) {
        appendLine("        " + row.joinToString("  ") { "${hex(it.start)}, ${hex(it.end)}, ${it.category}," })
    }
    appendLine("    )")
    appendLine("}")
}
if (checkOnly) {
    check(source == generated) { "Stale Unicode categories: run kotlinr scripts/generate-unicode-categories.main.kts" }
} else {
    path.writeText(generated)
}
println("${if (checkOnly) "Verified" else "Generated"} ${ranges.size} supplementary category ranges for Unicode $version.")
