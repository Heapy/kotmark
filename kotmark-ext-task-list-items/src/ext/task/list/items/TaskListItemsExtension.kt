package io.heapy.kotmark.ext.task.list.items

import io.heapy.kotmark.Extension
import io.heapy.kotmark.ExtensionsBuilder
import io.heapy.kotmark.ext.task.list.items.internal.TaskListItemHtmlNodeRenderer
import io.heapy.kotmark.ext.task.list.items.internal.TaskListItemMarkdownNodeRenderer
import io.heapy.kotmark.ext.task.list.items.internal.TaskListItemPostProcessor
import io.heapy.kotmark.parser.Parser
import io.heapy.kotmark.renderer.NodeRenderer
import io.heapy.kotmark.renderer.html.HtmlRenderer
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererContext
import io.heapy.kotmark.renderer.markdown.MarkdownNodeRendererFactory
import io.heapy.kotmark.renderer.markdown.MarkdownRenderer
import kotlin.jvm.JvmStatic

class TaskListItemsExtension private constructor() :
    Parser.ParserExtension,
    HtmlRenderer.HtmlRendererExtension,
    MarkdownRenderer.MarkdownRendererExtension {
    override fun extend(parserConfiguration: Parser.Configuration) {
        parserConfiguration.postProcessor(TaskListItemPostProcessor())
    }

    override fun extend(rendererConfiguration: HtmlRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(::TaskListItemHtmlNodeRenderer)
    }

    override fun extend(rendererConfiguration: MarkdownRenderer.Configuration) {
        rendererConfiguration.nodeRendererFactory(
            object : MarkdownNodeRendererFactory {
                override val specialCharacters: Set<Char> = emptySet()

                override fun create(context: MarkdownNodeRendererContext): NodeRenderer =
                    TaskListItemMarkdownNodeRenderer(context)
            },
        )
    }

    companion object {
        @JvmStatic
        fun create(): Extension = TaskListItemsExtension()
    }
}

/** Creates and appends [TaskListItemsExtension]. */
fun ExtensionsBuilder.taskListItems() {
    add(TaskListItemsExtension.create())
}
