package io.heapy.kotmark

/** Builds an ordered list of extensions to share between parsers and renderers. */
@KotmarkDsl
class ExtensionsBuilder internal constructor() {
    private val registrations = mutableListOf<Extension>()

    /** Appends [extension], preserving its identity and any repeated registrations. */
    fun add(extension: Extension) {
        registrations.add(extension)
    }

    /** Appends the current contents of [extensions] in iteration order. */
    fun addAll(extensions: Iterable<Extension>) {
        registrations.addAll(extensions)
    }

    internal fun build(): List<Extension> = registrations.toList()
}

/**
 * Creates a read-only snapshot of the registrations made by [configure].
 *
 * Pass the resulting list to each parser or renderer that needs it. Entries retain their
 * order and identity; extension objects and their callbacks are shared, not copied.
 * Later changes to a captured builder do not affect the returned list.
 */
fun extensions(configure: ExtensionsBuilder.() -> Unit = {}): List<Extension> =
    ExtensionsBuilder().apply(configure).build()
