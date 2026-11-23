package com.example

import com.example.util.EnglishTitleMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnglishTitleMapperTest {

    @Test
    fun curatedDonghuaPinyinResolvesToEnglishWithSeasonSuffix() {
        assertEquals(
            "Renegade Immortal",
            EnglishTitleMapper.resolve(titleEnglish = null, titleRomaji = "Xian Ni", synonyms = null, isDonghua = true)
        )
        assertEquals(
            "Battle Through the Heavens Season 5",
            EnglishTitleMapper.resolve(titleEnglish = null, titleRomaji = "Dou Po Cang Qiong Di 5 Ji", synonyms = null, isDonghua = true)
        )
        assertEquals(
            "Tales of Herding Gods",
            EnglishTitleMapper.resolve(titleEnglish = "Mu Shen Ji", titleRomaji = "Mu Shen Ji", synonyms = listOf("牧神记"), isDonghua = true)
        )
        assertEquals(
            "Lord of the Mysteries",
            EnglishTitleMapper.resolve(titleEnglish = null, titleRomaji = "Gui Mi Zhi Zhu", synonyms = listOf("LOTM"), isDonghua = true)
        )
    }

    @Test
    fun uncuratedDonghuaPicksCleanAlternateEnglishSynonymOverPinyinAndAcronyms() {
        val resolved = EnglishTitleMapper.resolve(
            titleEnglish = null,
            titleRomaji = "Shen Zhou Jian Ying",
            synonyms = listOf("神州剑影", "SZJY", "Shenzhou Jianying", "Shadow of the Divine Sword"),
            isDonghua = true
        )
        assertEquals("Shadow of the Divine Sword", resolved)
    }

    @Test
    fun pinyinIsDetectedAndRejectedForDonghuaOfficialEnglishSlot() {
        assertTrue(EnglishTitleMapper.isLikelyPinyin("Doupo Cangqiong", "Dou Po Cang Qiong"))
        assertFalse(EnglishTitleMapper.isLikelyPinyin("Battle Through the Heavens", "Dou Po Cang Qiong"))
        assertEquals(
            "A Record of a Mortal's Journey to Immortality",
            EnglishTitleMapper.resolveTitle(
                titleEnglish = "Fan Ren Xiu Xian Chuan",
                titleRomaji = "Fan Ren Xiu Xian Chuan",
                titleNative = "凡人修仙传",
                titlePinyin = "Fan Ren Xiu Xian Chuan",
                isDonghua = true,
                preference = "ENGLISH"
            )
        )
    }

    @Test
    fun keywordExpansionMapsEnglishKeywordsToDonghuaPinyin() {
        val expanded = EnglishTitleMapper.expandSearchQueries("renegade immortal", donghuaOnly = false, maxExtra = 2)
        assertTrue("Expected Xian Ni in $expanded", expanded.any { it.equals("Xian Ni", ignoreCase = true) })

        val herding = EnglishTitleMapper.expandSearchQueries("herding gods", donghuaOnly = true, maxExtra = 2)
        assertTrue("Expected Mu Shen Ji in $herding", herding.any { it.equals("Mu Shen Ji", ignoreCase = true) })
    }

    @Test
    fun keywordScoringRanksDirectWordMatchesHigherThanWeakMatches() {
        val direct = EnglishTitleMapper.scoreKeywordMatch(
            rawQuery = "swallowed star",
            resolvedEnglish = "Swallowed Star",
            officialEnglish = null,
            romaji = "Tunshi Xingkong",
            synonyms = listOf("Swallow the Starry Sky")
        )
        val partial = EnglishTitleMapper.scoreKeywordMatch(
            rawQuery = "swallowed star",
            resolvedEnglish = "Star Martial God Technique",
            officialEnglish = null,
            romaji = "Xing Wu Shen Jue",
            synonyms = emptyList()
        )
        assertTrue("Expected direct ($direct) > partial ($partial)", direct > partial)
        assertTrue(EnglishTitleMapper.matchesAllKeywords("heavens battle", "Battle Through the Heavens", "Dou Po Cang Qiong"))
        assertFalse(EnglishTitleMapper.matchesAllKeywords("heavens pirate", "Battle Through the Heavens", "Dou Po Cang Qiong"))
    }
}
