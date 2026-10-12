package com.finsim.ingestion

data class ScoredPage(
    val pageIndex: Int, // 1-indexed
    val score: Double,
    val matchedKeywordCount: Int,
    val wordCount: Int
)

/**
 * Scans a list of page texts and computes relevance and keyword density
 * for key financial indicators ('saldo', 'capital', 'tasa', 'interés', 'cuota', 'seguro', etc.)
 * to locate the primary statement summary page.
 */
object PageDensityScanner {

    private val KEYWORD_WEIGHTS: Map<String, Double> = mapOf(
        "saldo" to 4.0,
        "capital" to 4.0,
        "tasa" to 4.0,
        "cuota" to 4.0,
        "interes" to 3.5,
        "seguro" to 3.5,
        "seguros" to 3.5,
        "plazo" to 3.0,
        "prestamo" to 2.5,
        "credito" to 2.5,
        "amortizacion" to 3.0,
        "extracto" to 2.0,
        "obligacion" to 2.0,
        "vencimiento" to 2.0,
        "desembolso" to 2.5,
        "insoluto" to 3.0,
        "cat" to 2.5,
        "tna" to 2.5,
        "tea" to 2.5,
        "cft" to 2.5,
        "condusef" to 2.0,
        "sfc" to 2.0,
        "bcra" to 2.0,
        "cmf" to 2.0
    )

    private fun normalize(text: String): String {
        return text.lowercase()
            .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u')
            .replace('Á', 'a').replace('É', 'e').replace('Í', 'i').replace('Ó', 'o').replace('Ú', 'u')
            .replace('ü', 'u').replace('Ü', 'u')
            .replace('ñ', 'n').replace('Ñ', 'n')
    }

    /**
     * Scores all pages and returns the list of [ScoredPage] sorted by page index.
     */
    fun scorePages(pages: List<String>): List<ScoredPage> {
        return pages.mapIndexed { index, rawText ->
            val pageIndex = index + 1
            val normText = normalize(rawText)
            val words = normText.split(Regex("[^a-z0-9.]+")).filter { it.isNotBlank() }
            val wordCount = words.size

            if (wordCount == 0) {
                ScoredPage(pageIndex = pageIndex, score = 0.0, matchedKeywordCount = 0, wordCount = 0)
            } else {
                var totalWeight = 0.0
                var matchCount = 0

                KEYWORD_WEIGHTS.forEach { (keyword, weight) ->
                    val pattern = Regex("\\b${Regex.escape(keyword)}\\b")
                    val matches = pattern.findAll(normText).count()
                    if (matches > 0) {
                        matchCount += matches
                        totalWeight += matches * weight
                    }
                }

                val density = totalWeight / wordCount.toDouble()
                val score = (totalWeight * 4.0) + (density * 100.0)

                ScoredPage(
                    pageIndex = pageIndex,
                    score = score,
                    matchedKeywordCount = matchCount,
                    wordCount = wordCount
                )
            }
        }
    }

    /**
     * Evaluates pages and returns the 1-indexed page index of the highest-relevance page.
     * Defaults to 1 if no text or no keywords were matched.
     */
    fun findBestPageIndex(pages: List<String>): Int {
        if (pages.isEmpty()) return 1
        val scored = scorePages(pages)
        val best = scored.maxByOrNull { it.score } ?: return 1
        return if (best.score > 0.0) best.pageIndex else 1
    }
}
