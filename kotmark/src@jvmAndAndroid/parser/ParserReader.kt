package io.heapy.kotmark.parser

import java.io.Reader
import io.heapy.kotmark.node.Node

/**
 * Parse characters from [input]. The caller remains responsible for closing the reader.
 */
fun Parser.parseReader(input: Reader): Node = parse(input.readText())
