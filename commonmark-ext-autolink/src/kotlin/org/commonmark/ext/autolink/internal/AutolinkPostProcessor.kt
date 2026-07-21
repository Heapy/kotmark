package org.commonmark.ext.autolink.internal

import org.commonmark.ext.autolink.AutolinkType
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.Link
import org.commonmark.node.Node
import org.commonmark.node.SourceSpan
import org.commonmark.node.Text
import org.commonmark.parser.PostProcessor

class AutolinkPostProcessor(
    private val linkTypes: Set<AutolinkType>,
) : PostProcessor {
    init {
        require(linkTypes.isNotEmpty()) { "linkTypes must not be empty" }
    }

    override fun process(node: Node): Node {
        node.accept(AutolinkVisitor())
        return node
    }

    private fun linkify(originalTextNode: Text) {
        val literal = originalTextNode.literal.orEmpty()
        val links = findLinks(literal)
        if (links.isEmpty()) {
            return
        }
        val sourceSpan =
            originalTextNode.getSourceSpans().singleOrNull()
        var lastNode: Node = originalTextNode
        var index = 0
        for (link in links) {
            if (link.start > index) {
                lastNode =
                    insertNode(
                        createTextNode(literal, index, link.start, sourceSpan),
                        lastNode,
                    )
            }
            val textNode = createTextNode(literal, link.start, link.end, sourceSpan)
            val destination =
                when (link.type) {
                    AutolinkType.EMAIL -> "mailto:${textNode.literal}"
                    AutolinkType.WWW -> "http://${textNode.literal}"
                    AutolinkType.URL -> textNode.literal.orEmpty()
                }
            val linkNode = Link(destination, null)
            linkNode.appendChild(textNode)
            linkNode.setSourceSpans(textNode.getSourceSpans())
            lastNode = insertNode(linkNode, lastNode)
            index = link.end
        }
        if (index < literal.length) {
            insertNode(createTextNode(literal, index, literal.length, sourceSpan), lastNode)
        }
        originalTextNode.unlink()
    }

    private fun findLinks(literal: String): List<LinkMatch> {
        val candidates = mutableListOf<LinkMatch>()
        if (AutolinkType.URL in linkTypes) {
            URL_PATTERN.findAll(literal).forEach { match ->
                createUrlMatch(literal, match, AutolinkType.URL)?.let(candidates::add)
            }
        }
        if (AutolinkType.WWW in linkTypes) {
            WWW_PATTERN.findAll(literal).forEach { match ->
                createUrlMatch(literal, match, AutolinkType.WWW)?.let(candidates::add)
            }
        }
        if (AutolinkType.EMAIL in linkTypes) {
            EMAIL_PATTERN.findAll(literal).forEach { match ->
                val text = match.value
                val domain = text.substringAfter('@')
                if (
                    domain.lastOrNull() != '-' &&
                    domain.lastOrNull() != '_' &&
                    isValidDomain(domain, email = true)
                ) {
                    candidates.add(
                        LinkMatch(
                            match.range.first,
                            match.range.last + 1,
                            AutolinkType.EMAIL,
                        ),
                    )
                }
            }
        }

        val links = mutableListOf<LinkMatch>()
        var occupiedUntil = 0
        for (
            candidate in
                candidates.sortedWith(
                    compareBy<LinkMatch> { it.start }
                        .thenByDescending { it.end }
                        .thenBy { it.type.ordinal },
                )
        ) {
            if (candidate.start >= occupiedUntil) {
                links.add(candidate)
                occupiedUntil = candidate.end
            }
        }
        return links
    }

    private fun createUrlMatch(
        literal: String,
        match: MatchResult,
        type: AutolinkType,
    ): LinkMatch? {
        val start = match.range.first
        if (start > 0 && !isUrlBoundary(literal[start - 1])) {
            return null
        }
        val prefixLength =
            if (type == AutolinkType.WWW) {
                4
            } else {
                match.value.indexOf("://") + 3
            }
        val afterPrefix = match.value.substring(prefixLength)
        var domainEnd =
            afterPrefix.indexOfFirst {
                it != '.' && !it.isAsciiDomainCharacter()
            }
                .let { if (it == -1) afterPrefix.length else it }
        while (domainEnd > 0 && afterPrefix[domainEnd - 1] == '.') {
            domainEnd--
        }
        val domain = afterPrefix.substring(0, domainEnd)
        if (!isValidDomain(domain, email = false)) {
            return null
        }

        var end = match.range.last + 1
        while (end > start && literal[end - 1] in TRAILING_PUNCTUATION) {
            end--
        }
        if (end > start && literal[end - 1] == ')') {
            val value = literal.substring(start, end)
            var unmatchedClosing = value.count { it == ')' } - value.count { it == '(' }
            while (unmatchedClosing > 0 && end > start && literal[end - 1] == ')') {
                end--
                unmatchedClosing--
            }
        }
        if (end > start && literal[end - 1] == ';') {
            val ampersand = literal.lastIndexOf('&', end - 1)
            if (
                ampersand >= start &&
                ampersand + 1 < end - 1 &&
                literal.substring(ampersand + 1, end - 1).all(Char::isLetterOrDigit)
            ) {
                end = ampersand
            }
        }
        return if (end > start + prefixLength + domain.length) {
            LinkMatch(start, end, type)
        } else if (end == start + prefixLength + domain.length) {
            LinkMatch(start, end, type)
        } else {
            null
        }
    }

    private fun isValidDomain(domain: String, email: Boolean): Boolean {
        val segments = domain.split('.')
        if (segments.size < 2 || segments.any(String::isEmpty)) {
            return false
        }
        if (!email && segments.takeLast(2).any { '_' in it }) {
            return false
        }
        return segments.all { segment ->
            segment.all { character -> character.isAsciiDomainCharacter() }
        }
    }

    private fun Char.isAsciiDomainCharacter(): Boolean =
        this in 'a'..'z' ||
            this in 'A'..'Z' ||
            this in '0'..'9' ||
            this == '_' ||
            this == '-'

    private fun isUrlBoundary(character: Char): Boolean =
        character.isWhitespace() || character in "*_~(<"

    private fun createTextNode(
        literal: String,
        beginIndex: Int,
        endIndex: Int,
        sourceSpan: SourceSpan?,
    ): Text =
        Text(literal.substring(beginIndex, endIndex)).also { text ->
            sourceSpan?.let { text.addSourceSpan(it.subSpan(beginIndex, endIndex)) }
        }

    private fun insertNode(node: Node, insertAfterNode: Node): Node {
        insertAfterNode.insertAfter(node)
        return node
    }

    private inner class AutolinkVisitor : AbstractVisitor() {
        private var inLink = 0

        override fun visit(link: Link) {
            inLink++
            super.visit(link)
            inLink--
        }

        override fun visit(text: Text) {
            if (inLink == 0) {
                linkify(text)
            }
        }
    }

    private data class LinkMatch(
        val start: Int,
        val end: Int,
        val type: AutolinkType,
    )

    companion object {
        private val URL_PATTERN =
            Regex(
                "(?i)(?:https?://|ftp://)[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)+[^\\s<]*",
            )
        private val WWW_PATTERN =
            Regex("(?i)www\\.[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)+[^\\s<]*")
        private val EMAIL_PATTERN =
            Regex("[A-Za-z0-9._+-]+@[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)+")
        private const val TRAILING_PUNCTUATION = "?!.,:*_~"
    }
}
