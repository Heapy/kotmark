package org.commonmark.renderer.html

fun interface AttributeProviderFactory {
    fun create(context: AttributeProviderContext): AttributeProvider
}
