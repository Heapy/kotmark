package io.heapy.kotmark.parser

import io.heapy.kotmark.Extension
import io.heapy.kotmark.KotmarkDsl
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
import kotlin.reflect.KClass

/**
 * Parses Markdown input to a tree of nodes.
 */
class Parser private constructor(configuration: Configuration) {
    private val blockParserFactories =
        DocumentParser.calculateBlockParserFactories(
            configuration.blockParserFactories,
            configuration.enabledBlockTypes,
        )
    private val inlineContentParserFactories = configuration.inlineContentParserFactories.toList()
    private val delimiterProcessors = configuration.delimiterProcessors.toList()
    private val linkProcessors = configuration.linkProcessors.toList()
    private val linkMarkers = configuration.linkMarkers.toSet()
    private val inlineParserFactory = configuration.resolveInlineParserFactory()
    private val postProcessors = configuration.postProcessors.toList()
    private val includeSourceSpans = configuration.includeSourceSpans
    private val maxOpenBlockParsers = configuration.maxOpenBlockParsers

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

    @KotmarkDsl
    class Configuration internal constructor() {
        internal val blockParserFactories = mutableListOf<BlockParserFactory>()
        internal val inlineContentParserFactories = mutableListOf<InlineContentParserFactory>()
        internal val delimiterProcessors = mutableListOf<DelimiterProcessor>()
        internal val linkProcessors = mutableListOf<LinkProcessor>()
        internal val postProcessors = mutableListOf<PostProcessor>()
        internal val linkMarkers = mutableSetOf<Char>()
        var enabledBlockTypes: Set<KClass<out Block>> =
            DocumentParser.getDefaultBlockParserTypes()
            set(value) {
                DocumentParser.checkEnabledBlockTypes(value)
                field = value.toSet()
            }
        var inlineParserFactory: InlineParserFactory? = null
        var includeSourceSpans: IncludeSourceSpans = IncludeSourceSpans.NONE
        var maxOpenBlockParsers: Int = Int.MAX_VALUE
            set(value) {
                require(value >= 0) { "maxOpenBlockParsers must be >= 0" }
                field = value
            }

        internal fun create(): Parser = Parser(this)

        fun extensions(extensions: Iterable<Extension>) {
            for (extension in extensions) {
                if (extension is ParserExtension) {
                    extension.extend(this)
                }
            }
        }

        fun customBlockParserFactory(blockParserFactory: BlockParserFactory) {
            blockParserFactories.add(blockParserFactory)
        }

        fun customInlineContentParserFactory(
            inlineContentParserFactory: InlineContentParserFactory,
        ) {
            inlineContentParserFactories.add(inlineContentParserFactory)
        }

        fun customDelimiterProcessor(delimiterProcessor: DelimiterProcessor) {
            delimiterProcessors.add(delimiterProcessor)
        }

        fun linkProcessor(linkProcessor: LinkProcessor) {
            linkProcessors.add(linkProcessor)
        }

        fun linkMarker(linkMarker: Char) {
            linkMarkers.add(linkMarker)
        }

        fun postProcessor(postProcessor: PostProcessor) {
            postProcessors.add(postProcessor)
        }

        internal fun resolveInlineParserFactory(): InlineParserFactory =
            inlineParserFactory ?: InlineParserFactory(::InlineParserImpl)
    }

    fun interface ParserExtension : Extension {
        fun extend(parserConfiguration: Configuration)
    }
}

/** Creates [Parser] with the supplied configuration. */
fun parser(configure: Parser.Configuration.() -> Unit = {}): Parser =
    Parser.Configuration().apply(configure).create()
