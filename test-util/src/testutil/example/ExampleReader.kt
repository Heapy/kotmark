package io.heapy.kotmark.testutil.example

import io.heapy.kotmark.testfixtures.TestFixtures

class ExampleReader private constructor(
    private val input: String,
    private val filename: String,
) {
    private var state = State.BEFORE
    private var section = ""
    private var info = ""
    private var source = StringBuilder()
    private var html = StringBuilder()
    private var exampleNumber = 0
    private val examples = mutableListOf<Example>()

    private fun read(): List<Example> {
        for (line in input.lineSequence()) {
            processLine(line)
        }
        return examples
    }

    private fun processLine(line: String) {
        when (state) {
            State.BEFORE -> {
                SECTION_PATTERN.matchEntire(line)?.let { match ->
                    section = match.groupValues[1]
                    exampleNumber = 0
                }
                if (line.startsWith(EXAMPLE_START_MARKER)) {
                    info = line.substring(EXAMPLE_START_MARKER.length).trim()
                    state = State.SOURCE
                    exampleNumber++
                }
            }
            State.SOURCE ->
                if (line == ".") {
                    state = State.HTML
                } else {
                    source.append(line.replace('\u2192', '\t')).append('\n')
                }
            State.HTML ->
                if (line == EXAMPLE_END_MARKER) {
                    examples.add(
                        Example(
                            filename,
                            section,
                            info,
                            exampleNumber,
                            source.toString(),
                            html.toString(),
                        ),
                    )
                    source = StringBuilder()
                    html = StringBuilder()
                    state = State.BEFORE
                } else {
                    html.append(line).append('\n')
                }
        }
    }

    private enum class State {
        BEFORE,
        SOURCE,
        HTML,
    }

    companion object {
        private val SECTION_PATTERN = Regex("#{1,6} *(.*)")
        private const val EXAMPLE_START_MARKER =
            "```````````````````````````````` example"
        private const val EXAMPLE_END_MARKER =
            "````````````````````````````````"

        fun readExamples(path: String): List<Example> =
            ExampleReader(
                TestFixtures.readText(path),
                path.substringAfterLast('/'),
            ).read()

        fun readExamples(path: String, info: String): List<Example> =
            readExamples(path).filter { info in it.info }

        fun readExampleObjects(path: String, info: String): List<Array<Any>> =
            readExamples(path, info).map { arrayOf(it) }

        fun readExampleSources(path: String): List<String> =
            readExamples(path).map(Example::source)
    }
}
