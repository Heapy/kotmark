package io.heapy.kotmark.renderer.html

fun interface AttributeProviderFactory {
    fun create(context: AttributeProviderContext): AttributeProvider
}
