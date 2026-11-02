package io.heapy.kotmark

/** Keeps nested configuration blocks from implicitly modifying an outer configuration. */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class KotmarkDsl
