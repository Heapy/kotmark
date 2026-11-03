package io.heapy.kotmark.docs.examples.guides

// region combine-extensions
import io.heapy.kotmark.Extension
import io.heapy.kotmark.ext.gfm.tables.tables
import io.heapy.kotmark.extensions

fun combineExtensions(
    customExtension: Extension,
    existingExtensions: Iterable<Extension>,
): List<Extension> = extensions {
    add(customExtension)
    addAll(existingExtensions)
    tables { maxCells = 10_000 }
}
// endregion combine-extensions
