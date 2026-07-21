package org.commonmark.parser

import java.io.Reader
import org.commonmark.node.Node

/**
 * Parse characters from [input]. The caller remains responsible for closing the reader.
 */
fun Parser.parseReader(input: Reader): Node = parse(input.readText())
