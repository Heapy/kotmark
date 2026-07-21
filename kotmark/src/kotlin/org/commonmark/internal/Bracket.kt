package org.commonmark.internal

import org.commonmark.node.Text
import org.commonmark.parser.beta.Position
import kotlin.jvm.JvmStatic

/**
 * Opening bracket for links, images, or links with custom markers.
 */
class Bracket private constructor(
    val markerNode: Text?,
    val markerPosition: Position?,
    val bracketNode: Text,
    val bracketPosition: Position,
    val contentPosition: Position,
    val previous: Bracket?,
    val previousDelimiter: Delimiter?,
) {
    var allowed: Boolean = true
    var bracketAfter: Boolean = false

    companion object {
        @JvmStatic
        fun link(
            bracketNode: Text,
            bracketPosition: Position,
            contentPosition: Position,
            previous: Bracket?,
            previousDelimiter: Delimiter?,
        ): Bracket =
            Bracket(
                null,
                null,
                bracketNode,
                bracketPosition,
                contentPosition,
                previous,
                previousDelimiter,
            )

        @JvmStatic
        fun withMarker(
            markerNode: Text,
            markerPosition: Position,
            bracketNode: Text,
            bracketPosition: Position,
            contentPosition: Position,
            previous: Bracket?,
            previousDelimiter: Delimiter?,
        ): Bracket =
            Bracket(
                markerNode,
                markerPosition,
                bracketNode,
                bracketPosition,
                contentPosition,
                previous,
                previousDelimiter,
            )
    }
}
