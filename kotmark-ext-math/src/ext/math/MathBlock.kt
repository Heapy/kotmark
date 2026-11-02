package io.heapy.kotmark.ext.math

import io.heapy.kotmark.node.CustomBlock

/** Raw formula lines, excluding fences, with a trailing LF for each content line. */
class MathBlock(var literal: String = "") : CustomBlock()
