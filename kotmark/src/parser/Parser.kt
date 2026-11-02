package io.heapy.kotmark.parser

import io.heapy.kotmark.Extension
import io.heapy.kotmark.internal.Definitions
import io.heapy.kotmark.internal.DocumentParser
import io.heapy.kotmark.internal.InlineParserContextImpl
import io.heapy.kotmark.internal.InlineParserImpl
import io.heapy.kotmark.node.Block
import io.heapy.kotmark.node.Node
import io.heapy.kotmark.parser.beta.InlineContentParserFactory
import io.heapy.kotmark.parser.beta.LinkProcessor
import io.heapy.kotmark.parser.block.BlockParserFactory
import io.heapy.kotmark.parser.delimiter.DelimiterProcessor
import kotlin.jvm.JvmStatic
import kotlin.reflect.KClass

/**
 * Parses Markdown input to a tree of nodes.
 */
class Parser private constructor(builder: Builder) {
    private val blockParserFactories =
        DocumentParser.calculateBlockParserFactories(
            builder.blockParserFactories,
            builder.enabledBlockTypes,
        )
    private val inlineContentParserFactories = builder.inlineContentParserFactories.toList()
    private val delimiterProcessors = builder.delimiterProcessors.toList()
    private val linkProcessors = builder.linkProcessors.toList()
    private val linkMarkers = builder.linkMarkers.toSet()
    private val inlineParserFactory = builder.resolveInlineParserFactory()
    private val postProcessors = builder.postProcessors.toList()
    private val includeSourceSpans = builder.includeSourceSpans
    private val maxOpenBlockParsers = builder.maxOpenBlockParsers

    init {
        val context =
            InlineParserContextImpl(
                inlineContentParserFactories,
                delimiterProcessors,
                linkProcessors,
                linkMarkers,
                Definitions(),
            )
        inlineParserFactory.create(context)
    }

    /**
     * Parse [input]. A new parser state is used for each call.
     */
    fun parse(input: String): Node {
        val documentParser = createDocumentParser()
        return postProcess(documentParser.parse(input))
    }

    private fun createDocumentParser(): DocumentParser =
        DocumentParser(
            blockParserFactories,
            inlineParserFactory,
            inlineContentParserFactories,
            delimiterProcessors,
            linkProcessors,
            linkMarkers,
            includeSourceSpans,
            maxOpenBlockParsers,
        )

    private fun postProcess(document: Node): Node {
        var result = document
        for (postProcessor in postProcessors) {
            result = postProcessor.process(result)
        }
        return result
    }

    class Builder internal constructor() {
        internal val blockParserFactories = mutableListOf<BlockParserFactory>()
        internal val inlineContentParserFactories = mutableListOf<InlineContentParserFactory>()
        internal val delimiterProcessors = mutableListOf<DelimiterProcessor>()
        internal val linkProcessors = mutableListOf<LinkProcessor>()
        internal val postProcessors = mutableListOf<PostProcessor>()
        internal val linkMarkers = mutableSetOf<Char>()
        internal var enabledBlockTypes: Set<KClass<out Block>> =
            DocumentParser.getDefaultBlockParserTypes()
        internal var inlineParserFactory: InlineParserFactory? = null
        internal var includeSourceSpans: IncludeSourceSpans = IncludeSourceSpans.NONE
        internal var maxOpenBlockParsers: Int = Int.MAX_VALUE

        fun build(): Parser = Parser(this)

        fun extensions(extensions: Iterable<Extension>): Builder = apply {
            for (extension in extensions) {
                if (extension is ParserExtension) {
                    extension.extend(this)
                }
            }
        }

        fun enabledBlockTypes(enabledBlockTypes: Set<KClass<out Block>>): Builder = apply {
            DocumentParser.checkEnabledBlockTypes(enabledBlockTypes)
            this.enabledBlockTypes = enabledBlockTypes
        }

        fun includeSourceSpans(includeSourceSpans: IncludeSourceSpans): Builder = apply {
            this.includeSourceSpans = includeSourceSpans
        }

        fun maxOpenBlockParsers(maxOpenBlockParsers: Int): Builder = apply {
            require(maxOpenBlockParsers >= 0) {
                "maxOpenBlockParsers must be >= 0"
            }
            this.maxOpenBlockParsers = maxOpenBlockParsers
        }

        fun customBlockParserFactory(blockParserFactory: BlockParserFactory): Builder = apply {
            blockParserFactories.add(blockParserFactory)
        }

        fun customInlineContentParserFactory(
            inlineContentParserFactory: InlineContentParserFactory,
        ): Builder = apply {
            inlineContentParserFactories.add(inlineContentParserFactory)
        }

        fun customDelimiterProcessor(delimiterProcessor: DelimiterProcessor): Builder = apply {
            delimiterProcessors.add(delimiterProcessor)
        }

        fun linkProcessor(linkProcessor: LinkProcessor): Builder = apply {
            linkProcessors.add(linkProcessor)
        }

        fun linkMarker(linkMarker: Char): Builder = apply {
            linkMarkers.add(linkMarker)
        }

        fun postProcessor(postProcessor: PostProcessor): Builder = apply {
            postProcessors.add(postProcessor)
        }

        fun inlineParserFactory(inlineParserFactory: InlineParserFactory?): Builder = apply {
            this.inlineParserFactory = inlineParserFactory
        }

        internal fun resolveInlineParserFactory(): InlineParserFactory =
            inlineParserFactory ?: InlineParserFactory(::InlineParserImpl)
    }

    fun interface ParserExtension : Extension {
        fun extend(parserBuilder: Builder)
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }
}
