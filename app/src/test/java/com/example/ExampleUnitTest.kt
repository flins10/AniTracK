package com.example

import com.example.data.local.AnimeEntity
import com.example.util.EnglishTitleMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testAcceptanceCase1_EnglishAndRomajiBothExist_DisplaysEnglish() {
    val entity = AnimeEntity(
      id = 100,
      titleEnglish = "Frieren: Beyond Journey's End",
      titleRomaji = "Sousou no Frieren",
      coverImage = "",
      airingDayOfWeek = 1
    )
    assertEquals("Frieren: Beyond Journey's End", entity.displayTitle)
  }

  @Test
  fun testAcceptanceCase2_EnglishIsNull_DisplaysRomaji() {
    val entity = AnimeEntity(
      id = 101,
      titleEnglish = null,
      titleRomaji = "Unlocalized Show",
      coverImage = "",
      airingDayOfWeek = 1
    )
    assertEquals("Unlocalized Show", entity.displayTitle)
  }

  @Test
  fun testAcceptanceCase3_EnglishIsEmptyString_DisplaysRomaji() {
    val entity = AnimeEntity(
      id = 102,
      titleEnglish = "  ",
      titleRomaji = "Unlocalized Show",
      coverImage = "",
      airingDayOfWeek = 1
    )
    assertEquals("Unlocalized Show", entity.displayTitle)
  }

  @Test
  fun testAcceptanceCase4_EnglishInAniList_MalProvidesRomanized_DisplaysAniListEnglish() {
    // AniList provides English: "Link Click", MAL provides Romanized: "Shiguang Dailiren"
    val entity = AnimeEntity(
      id = 103,
      idMal = 44074,
      titleEnglish = "Link Click",
      titleRomaji = "Shiguang Dailiren",
      coverImage = "",
      airingDayOfWeek = 1
    )
    assertEquals("Link Click", entity.displayTitle)
  }

  @Test
  fun testAcceptanceCase5_DonghuaWithAvailableEnglishTitle_DisplaysEnglish() {
    val donghua = AnimeEntity(
      id = 104,
      titleEnglish = "Soul Land 2",
      titleRomaji = "Douluo Dalu II: Jueshi Tangmen",
      titlePinyin = "Douluo Dalu 2",
      countryOfOrigin = "CN",
      coverImage = "",
      airingDayOfWeek = 1
    )
    assertEquals("Soul Land 2", donghua.displayTitle)
  }

  @Test
  fun testAcceptanceCase6_RefreshingOrCopyingData_DoesNotRevertToRomaji() {
    val entity = AnimeEntity(
      id = 105,
      titleEnglish = "Solo Leveling",
      titleRomaji = "Ore dake Level Up na Ken",
      coverImage = "",
      airingDayOfWeek = 1
    )
    // Simulate cache/refresh update
    val refreshed = entity.copy(
      currentEpisode = 12,
      lastCachedAt = System.currentTimeMillis()
    )
    assertEquals("Solo Leveling", refreshed.displayTitle)
  }

  @Test
  fun testAcceptanceCase7_CentralizedTitleSelection_ConsistentAcrossScreens() {
    val entity = AnimeEntity(
      id = 106,
      titleEnglish = "Demon Slayer: Kimetsu no Yaiba",
      titleRomaji = "Kimetsu no Yaiba",
      coverImage = "",
      airingDayOfWeek = 1
    )
    val titleForCard = entity.displayTitle
    val titleForCalendar = entity.getResolvedTitle("ENGLISH")
    val titleForSearch = entity.displayTitle
    val titleForWishlist = entity.displayTitle

    assertEquals("Demon Slayer: Kimetsu no Yaiba", titleForCard)
    assertEquals(titleForCard, titleForCalendar)
    assertEquals(titleForCard, titleForSearch)
    assertEquals(titleForCard, titleForWishlist)
  }
}
