package io.heapy.kotmark.testfixtures

import io.heapy.kotmark.testfixtures.generated.embeddedFixture

/** Complete pinned fixture text available on every target without filesystem or network access. */
object TestFixtures {
    fun readText(path: String): String = embeddedFixture(path)
}
