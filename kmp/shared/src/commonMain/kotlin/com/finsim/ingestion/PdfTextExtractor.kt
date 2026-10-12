package com.finsim.ingestion

/**
 * Interface for extracting plain text per page from PDF bytes or platform documents.
 */
interface PdfTextExtractor {
    /**
     * Extracts text page by page from the supplied raw bytes.
     * Returns a list of plain text strings, one per page (1-indexed semantics).
     */
    fun extractPages(pdfBytes: ByteArray): List<String>
}

/**
 * Platform-agnostic PDF digital text extractor that parses plain text or
 * uncompressed PDF text stream operators (BT...ET, Tj, TJ).
 */
object DigitalPdfTextExtractor : PdfTextExtractor {

    override fun extractPages(pdfBytes: ByteArray): List<String> {
        if (pdfBytes.isEmpty()) return emptyList()

        val text = try {
            pdfBytes.decodeToString()
        } catch (_: Exception) {
            return emptyList()
        }

        // Check if raw plain text was passed directly (not a binary PDF)
        if (!text.startsWith("%PDF")) {
            val pages = text.split(Regex("(?:\u000C|---+\\s*(?:Page|Página)\\s*\\d+\\s*---+)", RegexOption.IGNORE_CASE))
                .map { it.trim() }
                .filter { it.isNotBlank() }
            return if (pages.isNotEmpty()) pages else listOf(text.trim())
        }

        // Digital PDF parsing: scan for Page objects and text operators (BT ... ET)
        return extractFromPdfText(text)
    }

    private fun extractFromPdfText(pdfContent: String): List<String> {
        val pages = mutableListOf<String>()

        // Split into pages by /Type /Page if present
        val pageSplits = pdfContent.split(Regex("""/Type\s*/Page\b"""))
        if (pageSplits.size > 1) {
            for (i in 1 until pageSplits.size) {
                val pageChunk = pageSplits[i]
                val extracted = extractTextFromChunk(pageChunk)
                if (extracted.isNotBlank()) {
                    pages.add(extracted)
                }
            }
        }

        if (pages.isEmpty()) {
            val entireExtracted = extractTextFromChunk(pdfContent)
            if (entireExtracted.isNotBlank()) {
                pages.add(entireExtracted)
            }
        }

        return if (pages.isNotEmpty()) pages else listOf(pdfContent.take(1000))
    }

    private fun extractTextFromChunk(chunk: String): String {
        val sb = StringBuilder()

        // 1. Extract strings from BT ... ET text blocks
        val btBlocks = Regex("""BT\s+(.*?)\s+ET""", RegexOption.DOT_MATCHES_ALL).findAll(chunk)
        for (block in btBlocks) {
            val blockContent = block.groupValues[1]

            // Match (String) Tj, (String) ', (String) "
            val tjMatches = Regex("""\(((?:[^()\\]|\\.)*)\)\s*(?:Tj|'|")""").findAll(blockContent)
            for (m in tjMatches) {
                val decoded = unescapePdfString(m.groupValues[1])
                sb.append(decoded).append(" ")
            }

            // Match [(Str1) -10 (Str2)] TJ
            val arrayMatches = Regex("""\[(.*?)\]\s*TJ""").findAll(blockContent)
            for (arr in arrayMatches) {
                val inner = arr.groupValues[1]
                val strParts = Regex("""\(((?:[^()\\]|\\.)*)\)""").findAll(inner)
                for (sp in strParts) {
                    sb.append(unescapePdfString(sp.groupValues[1]))
                }
                sb.append(" ")
            }
            sb.append("\n")
        }

        // Fallback: If no BT blocks were extracted, match any literal strings ( ... ) in streams
        if (sb.isBlank()) {
            val literalMatches = Regex("""\(((?:[^()\\]|\\.)*)\)""").findAll(chunk)
            for (m in literalMatches) {
                val str = unescapePdfString(m.groupValues[1]).trim()
                if (str.length > 2 && !str.startsWith("/")) {
                    sb.append(str).append(" ")
                }
            }
        }

        return sb.toString().trim()
    }

    private fun unescapePdfString(raw: String): String {
        return raw.replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }
}

/**
 * In-memory test and simulation extractor that returns predefined pages
 * or delegates to [DigitalPdfTextExtractor].
 */
class InMemoryPdfTextExtractor(
    private val predefinedPages: List<String> = emptyList()
) : PdfTextExtractor {

    override fun extractPages(pdfBytes: ByteArray): List<String> {
        if (predefinedPages.isNotEmpty()) {
            return predefinedPages
        }
        return DigitalPdfTextExtractor.extractPages(pdfBytes)
    }
}
