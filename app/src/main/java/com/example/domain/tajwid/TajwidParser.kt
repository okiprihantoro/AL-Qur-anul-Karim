package com.example.domain.tajwid

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.example.ui.theme.TajwidColors

enum class TajwidRule(
    val title: String,
    val summary: String,
    val exampleArabic: String
) {
    GHUNNAH(
        title = "Ghunnah (Dengung)",
        summary = "Nun atau Mim bertasydid (نّ / مّ) dibaca mendengung selama 2 harakat.",
        exampleArabic = "إِنَّ • ثُمَّ"
    ),
    IDGHAM_BIGHUNNAH(
        title = "Idgham Bighunnah",
        summary = "Nun sukun/tanwin bertemu huruf ي ن م و dibaca melebur disertai dengung.",
        exampleArabic = "مَنْ يَقُولُ • هُدًى مِنْ"
    ),
    IDGHAM_BILAGHUNNAH(
        title = "Idgham Bilaghunnah",
        summary = "Nun sukun/tanwin bertemu huruf ل atau ر dibaca melebur tanpa dengung.",
        exampleArabic = "مِنْ لَدُنْهُ • غَفُورٌ رَحِيمٌ"
    ),
    IQLAB(
        title = "Iqlab",
        summary = "Nun sukun/tanwin bertemu huruf ب diubah bunyinya menjadi Mim dengan dengung.",
        exampleArabic = "مِنْ بَعْدِ • سَمِيعٌ بَصِيرٌ"
    ),
    IKHFA(
        title = "Ikhfa (Samar)",
        summary = "Nun sukun/tanwin bertemu 15 huruf Ikhfa atau Mim sukun bertemu ب dibaca samar mendengung.",
        exampleArabic = "مِنْ شَرِّ • أَنْزَلْنَا"
    ),
    QALQALAH(
        title = "Qalqalah (Pantulan)",
        summary = "Huruf ق ط ب ج د yang berharakat sukun atau diwaqafkan dibaca memantul.",
        exampleArabic = "يَخْلُقْ • أَحَدٌ"
    ),
    MAD(
        title = "Mad (Panjang)",
        summary = "Bacaan panjang (Mad Wajib/Jaiz/Lazim) bertanda harakat panjang (~) dibaca 4–6 harakat.",
        exampleArabic = "السَّمَاءِ • وَلَا الضَّالِّينَ"
    );

    fun color(isDark: Boolean): Color = when (this) {
        GHUNNAH -> if (isDark) TajwidColors.GhunnahDark else TajwidColors.GhunnahLight
        IDGHAM_BIGHUNNAH -> if (isDark) TajwidColors.IdghamBighunnahDark else TajwidColors.IdghamBighunnahLight
        IDGHAM_BILAGHUNNAH -> if (isDark) TajwidColors.IdghamBilaghunnahDark else TajwidColors.IdghamBilaghunnahLight
        IQLAB -> if (isDark) TajwidColors.IqlabDark else TajwidColors.IqlabLight
        IKHFA -> if (isDark) TajwidColors.IkhfaDark else TajwidColors.IkhfaLight
        QALQALAH -> if (isDark) TajwidColors.QalqalahDark else TajwidColors.QalqalahLight
        MAD -> if (isDark) TajwidColors.MadDark else TajwidColors.MadLight
    }
}

object TajwidParser {

    private const val SHADDAH = '\u0651'
    private const val SUKUN = '\u0652'
    private const val SUKUN_UTHMANI = '\u06E1'
    private const val MADDAH = '\u0653'
    private const val FATHATAN = '\u064B'
    private const val DAMMATAN = '\u064C'
    private const val KASRATAN = '\u064D'
    private const val SUPERSCRIPT_ALIF = '\u0670'
    private const val SMALL_HIGH_MEEM = '\u06E2'

    private val qalqalahLetters = setOf('ق', 'ط', 'ب', 'ج', 'د')
    private val idghamBighunnahLetters = setOf('ي', 'ن', 'م', 'و')
    private val idghamBilaghunnahLetters = setOf('ل', 'ر')
    private val ikhfaLetters = setOf(
        'ت', 'ث', 'ج', 'د', 'ذ', 'ز', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ف', 'ق', 'ك'
    )

    private fun isDiacritic(c: Char): Boolean {
        val code = c.code
        return (code in 0x064B..0x065F) ||
            code == 0x0670 ||
            (code in 0x06D6..0x06ED)
    }

    private fun isSukun(c: Char): Boolean = c == SUKUN || c == SUKUN_UTHMANI

    private fun isTanwin(c: Char): Boolean = c == FATHATAN || c == DAMMATAN || c == KASRATAN

    fun parse(
        arabicText: String,
        enabled: Boolean,
        isDark: Boolean,
        defaultColor: Color
    ): AnnotatedString {
        if (!enabled || arabicText.isBlank()) {
            return AnnotatedString(arabicText)
        }

        val charColors = Array<TajwidRule?>(arabicText.length) { null }
        val n = arabicText.length

        var i = 0
        while (i < n) {
            val baseChar = arabicText[i]
            if (isDiacritic(baseChar) || baseChar.isWhitespace()) {
                i++
                continue
            }

            // Collect cluster: baseChar + following diacritics
            var clusterEnd = i + 1
            var hasShaddah = false
            var hasSukun = false
            var hasTanwin = false
            var hasMaddah = false
            var hasSmallMeem = false

            while (clusterEnd < n && isDiacritic(arabicText[clusterEnd])) {
                val d = arabicText[clusterEnd]
                if (d == SHADDAH) hasShaddah = true
                if (isSukun(d)) hasSukun = true
                if (isTanwin(d)) hasTanwin = true
                if (d == MADDAH || d == SUPERSCRIPT_ALIF) hasMaddah = true
                if (d == SMALL_HIGH_MEEM) hasSmallMeem = true
                clusterEnd++
            }

            // Find next base Arabic letter (skipping spaces and non-letter marks like Alif Wasl/tatweel)
            val nextInfo = findNextBaseLetter(arabicText, clusterEnd)

            // 1. Maddah (~)
            if (arabicText.substring(i, clusterEnd).contains(MADDAH)) {
                paintRange(charColors, i, clusterEnd, TajwidRule.MAD)
            }

            // 2. Ghunnah: Nun or Mim with Shaddah
            if ((baseChar == 'ن' || baseChar == 'م') && hasShaddah) {
                paintRange(charColors, i, clusterEnd, TajwidRule.GHUNNAH)
            }

            // 3. Qalqalah: Qaf, Ta, Ba, Jim, Dal with sukun or final position in ayah
            if (baseChar in qalqalahLetters && (hasSukun || nextInfo == null)) {
                paintRange(charColors, i, clusterEnd, TajwidRule.QALQALAH)
            }

            // 4. Nun Sukun or Tanwin rules
            val isNunSukun = (baseChar == 'ن' && hasSukun) ||
                (baseChar == 'ن' && clusterEnd == i + 1 && nextInfo != null && nextInfo.letter != 'ن')
            // Note: In Uthmani script, Nun without harakat before Idgham/Ikhfa has clusterEnd == i + 1
            val isSilentNun = baseChar == 'ن' && !hasShaddah && (hasSukun || clusterEnd == i + 1 || hasSmallMeem)
            if ((isSilentNun || hasTanwin) && nextInfo != null) {
                val nextLetter = nextInfo.letter
                val rule = when {
                    nextLetter == 'ب' || hasSmallMeem -> TajwidRule.IQLAB
                    nextLetter in idghamBighunnahLetters -> TajwidRule.IDGHAM_BIGHUNNAH
                    nextLetter in idghamBilaghunnahLetters -> TajwidRule.IDGHAM_BILAGHUNNAH
                    nextLetter in ikhfaLetters -> TajwidRule.IKHFA
                    else -> null
                }
                if (rule != null) {
                    paintRange(charColors, i, clusterEnd, rule)
                    // Also highlight the following merging/target letter cluster for Idgham/Iqlab
                    if (rule == TajwidRule.IDGHAM_BIGHUNNAH || rule == TajwidRule.IDGHAM_BILAGHUNNAH) {
                        paintRange(charColors, nextInfo.startIndex, nextInfo.endIndex, rule)
                    }
                }
            }

            // 5. Mim Sukun rules (Ikhfa Syafawi with Ba, Idgham Mimi with Mim)
            val isSilentMim = baseChar == 'م' && !hasShaddah && (hasSukun || clusterEnd == i + 1)
            if (isSilentMim && nextInfo != null) {
                when (nextInfo.letter) {
                    'ب' -> paintRange(charColors, i, nextInfo.endIndex, TajwidRule.IKHFA)
                    'م' -> paintRange(charColors, i, nextInfo.endIndex, TajwidRule.GHUNNAH)
                }
            }

            // 6. Superscript Alif on long vowels (subtle Mad indicator when Maddah isn't present)
            if (hasMaddah && charColors[i] == null && (baseChar == 'ا' || baseChar == 'و' || baseChar == 'ي' || baseChar == 'ى')) {
                paintRange(charColors, i, clusterEnd, TajwidRule.MAD)
            }

            i = clusterEnd
        }

        return buildAnnotatedString {
            append(arabicText)
            var runStart = 0
            while (runStart < n) {
                val rule = charColors[runStart]
                var runEnd = runStart + 1
                while (runEnd < n && charColors[runEnd] == rule) {
                    runEnd++
                }
                if (rule != null) {
                    addStyle(
                        style = SpanStyle(color = rule.color(isDark)),
                        start = runStart,
                        end = runEnd
                    )
                } else {
                    addStyle(
                        style = SpanStyle(color = defaultColor),
                        start = runStart,
                        end = runEnd
                    )
                }
                runStart = runEnd
            }
        }
    }

    private data class NextLetterInfo(
        val letter: Char,
        val startIndex: Int,
        val endIndex: Int
    )

    private fun findNextBaseLetter(text: String, fromIndex: Int): NextLetterInfo? {
        var j = fromIndex
        val n = text.length
        while (j < n) {
            val c = text[j]
            if (c.isWhitespace() || isDiacritic(c) || c == 'ا' && j == fromIndex && c == '\u0627' && j + 1 < n && isDiacritic(text[j + 1]).not() && text[j + 1].isWhitespace()) {
                j++
                continue
            }
            // Skip silent alif after tanwin fathatan (e.g., كِتَابًا م...)
            if (c == 'ا' && fromIndex > 0 && text[fromIndex - 1] == FATHATAN) {
                j++
                continue
            }
            if (c in '\u0621'..'\u064A' || c == 'ٱ') {
                var end = j + 1
                while (end < n && isDiacritic(text[end])) {
                    end++
                }
                return NextLetterInfo(c, j, end)
            }
            j++
        }
        return null
    }

    private fun paintRange(arr: Array<TajwidRule?>, start: Int, end: Int, rule: TajwidRule) {
        for (idx in start until end.coerceAtMost(arr.size)) {
            arr[idx] = rule
        }
    }
}
