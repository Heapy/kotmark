package io.heapy.kotmark.node

/**
 * A list block like [BulletList] or [OrderedList].
 */
abstract class ListBlock : Block() {
    var isTight: Boolean = false
}
