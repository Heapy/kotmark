package io.heapy.kotmark.ext.front.matter

import io.heapy.kotmark.node.CustomNode

/** YAML content without its opening and closing delimiters, with line endings normalized to `\n`. */
class YamlFrontMatterContent(var content: String) : CustomNode()
