package io.heapy.kotmark.internal

import io.heapy.kotmark.parser.block.BlockContinue

class BlockContinueImpl(
    val newIndex: Int,
    val newColumn: Int,
    val isFinalize: Boolean,
) : BlockContinue()
