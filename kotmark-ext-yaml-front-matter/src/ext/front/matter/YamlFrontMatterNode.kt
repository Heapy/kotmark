package io.heapy.kotmark.ext.front.matter

import io.heapy.kotmark.node.CustomNode

class YamlFrontMatterNode(
    var key: String,
    var values: List<String>,
) : CustomNode()
