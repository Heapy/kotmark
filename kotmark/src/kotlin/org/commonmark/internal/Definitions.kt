package org.commonmark.internal

import org.commonmark.node.DefinitionMap
import kotlin.reflect.KClass

class Definitions {
    private val definitionsByType: MutableMap<KClass<*>, DefinitionMap<*>> = mutableMapOf()

    fun <D : Any> addDefinitions(definitionMap: DefinitionMap<D>) {
        val existingMap = getMap(definitionMap.type)
        if (existingMap == null) {
            definitionsByType[definitionMap.type] = definitionMap
        } else {
            existingMap.addAll(definitionMap)
        }
    }

    fun <D : Any> getDefinition(type: KClass<D>, label: String): D? =
        getMap(type)?.get(label)

    @Suppress("UNCHECKED_CAST")
    private fun <D : Any> getMap(type: KClass<D>): DefinitionMap<D>? =
        definitionsByType[type] as DefinitionMap<D>?
}
