package org.commonmark.testutil.example

data class Example(
    private val filename: String,
    val section: String,
    val info: String,
    val exampleNumber: Int,
    val source: String,
    val html: String,
) {
    override fun toString(): String =
        "File \"$filename\" section \"$section\" example $exampleNumber"
}
