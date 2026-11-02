package io.heapy.kotmark.node

import io.heapy.kotmark.internal.util.Escaping
import kotlin.reflect.KClass

/**
 * A map that can be used to store and look up reference definitions by a label. The labels are case-insensitive and
 * normalized, the same way as for [LinkReferenceDefinition] nodes.
 */
class DefinitionMap<D : Any>(
    val type: KClass<D>,
) {

    // LinkedHashMap for determinism and to preserve document order
    private val definitions: MutableMap<String, D> = LinkedHashMap()

    fun addAll(that: DefinitionMap<D>) {
        // Note that keys are already normalized, so we can add them directly
        for ((key, value) in that.definitions) {
            if (definitions[key] == null) {
                definitions[key] = value
            }
        }
    }

    /**
     * Store a new definition unless one is already in the map. Returns null if added, otherwise returns the existing
     * definition. The label is normalized before storing.
     */
    @IgnorableReturnValue
    fun putIfAbsent(label: String, definition: D): D? {
        val normalizedLabel = Escaping.normalizeLabelContent(label)
        // spec: When there are multiple matching link reference definitions, the first is used
        val existing = definitions[normalizedLabel]
        if (existing == null) {
            definitions[normalizedLabel] = definition
        }
        return existing
    }

    /** Look up a definition by label. Returns null if not found. */
    fun get(label: String): D? {
        val normalizedLabel = Escaping.normalizeLabelContent(label)
        return definitions[normalizedLabel]
    }

    fun keySet(): Set<String> = definitions.keys

    fun values(): Collection<D> = definitions.values
}
