package com.example.desktop

import java.text.Normalizer

internal data class DesktopTitleEntry(
    val pinyinOrRomaji: String,
    val englishTitle: String,
    val alternateEnglish: List<String> = emptyList(),
    val isDonghua: Boolean = false
)

internal object DesktopEnglishTitleMapper {

    val entries: List<DesktopTitleEntry> = listOf(
        // Chinese Donghua (Curated Official & Alternate English Titles)
        DesktopTitleEntry("Wanmei Shijie", "Perfect World", listOf("Perfect World Donghua"), true),
        DesktopTitleEntry("Xian Ni", "Renegade Immortal", listOf("Immortal Renegade", "Wang Lin"), true),
        DesktopTitleEntry("Tunshi Xingkong", "Swallowed Star", listOf("Swallow the Starry Sky", "Devouring the Starry Sky"), true),
        DesktopTitleEntry("Douluo Dalu 2: Jueshi Tangmen", "Soul Land 2: The Peerless Tang Clan", listOf("Unrivaled Tang Sect", "Soul Land II"), true),
        DesktopTitleEntry("Douluo Dalu II", "Soul Land 2: The Peerless Tang Clan", listOf("Soul Land 2", "Peerless Tang Clan"), true),
        DesktopTitleEntry("Douluo Dalu", "Soul Land", listOf("Combat Continent", "Douluo Continent"), true),
        DesktopTitleEntry("Dou Po Cang Qiong", "Battle Through the Heavens", listOf("Fights Break Sphere", "Fight Break Sphere", "BTTH"), true),
        DesktopTitleEntry("Doupo Cangqiong", "Battle Through the Heavens", listOf("Fights Break Sphere", "BTTH"), true),
        DesktopTitleEntry("Fan Ren Xiu Xian Chuan", "A Record of a Mortal's Journey to Immortality", listOf("Mortal's Journey to Immortality", "Record of a Mortal's Journey to Immortality", "RMJI"), true),
        DesktopTitleEntry("Fanren Xiuxian Zhuan", "A Record of a Mortal's Journey to Immortality", listOf("Mortal's Journey to Immortality"), true),
        DesktopTitleEntry("Zhe Tian", "Shrouding the Heavens", listOf("Shroud the Heavens", "Covering the Sky"), true),
        DesktopTitleEntry("Shen Yin Wang Zuo", "Throne of Seal", listOf("Sealed Divine Throne", "Divine Throne"), true),
        DesktopTitleEntry("Shenyin Wangzuo", "Throne of Seal", listOf("Sealed Divine Throne"), true),
        DesktopTitleEntry("Mu Shen Ji", "Tales of Herding Gods", listOf("Herding Gods", "Record of Herding Gods"), true),
        DesktopTitleEntry("Jian Lai", "Sword of Coming", listOf("Sword Come", "Unsheathed"), true),
        DesktopTitleEntry("Gui Mi Zhi Zhu", "Lord of the Mysteries", listOf("Lord of Mysteries", "LOTM"), true),
        DesktopTitleEntry("Guimi Zhi Zhu", "Lord of the Mysteries", listOf("Lord of Mysteries"), true),
        DesktopTitleEntry("Shi Xiong A Shi Xiong", "Big Brother", listOf("My Senior Brother Is Too Steady", "Senior Brother Is Too Steady"), true),
        DesktopTitleEntry("Cang Yuan Tu", "The Demon Hunter", listOf("Archean Eon Art"), true),
        DesktopTitleEntry("Bai Lian Cheng Shen", "Apotheosis", listOf("Elevation to the Status of a God", "Refining into a God"), true),
        DesktopTitleEntry("Wu Shen Zhu Zai", "Martial Master", listOf("Dominant Martial God", "God of Martial Arts"), true),
        DesktopTitleEntry("Lian Qi Shi Wan Nian", "One Hundred Thousand Years of Qi Refining", listOf("100,000 Years of Qi Refining", "100000 Years of Qi Refining"), true),
        DesktopTitleEntry("Zhu Xian", "Jade Dynasty", listOf("Legend of Chusen"), true),
        DesktopTitleEntry("Da Zhu Zai", "The Great Ruler", listOf("Great Ruler"), true),
        DesktopTitleEntry("Ni Tian Xie Shen", "Against the Gods", listOf("Against the Sky Supreme"), true),
        DesktopTitleEntry("Ni Tian Zhizun", "Against the Sky Supreme", listOf("Against the Gods", "Supreme Against the Sky"), true),
        DesktopTitleEntry("Shiguang Dailiren", "Link Click", listOf("Time Agent", "Time Agents"), true),
        DesktopTitleEntry("Tian Guan Ci Fu", "Heaven Official's Blessing", listOf("Heaven Official Blessing", "TGCF"), true),
        DesktopTitleEntry("Mo Dao Zu Shi", "The Founder of Diabolism", listOf("Grandmaster of Demonic Cultivation", "The Grandmaster of Demonic Cultivation", "MDZS"), true),
        DesktopTitleEntry("Yi Nian Yong Heng", "A Will Eternal", listOf("One Thought Eternity", "Will Eternal"), true),
        DesktopTitleEntry("Wu Dong Qian Kun", "Martial Universe", listOf("Martial Movement Upheaval"), true),
        DesktopTitleEntry("Xing Chen Bian", "Stellar Transformations", listOf("Stellar Transformation", "Legend of Immortals"), true),
        DesktopTitleEntry("Yao Shen Ji", "Tales of Demons and Gods", listOf("Demons and Gods"), true),
        DesktopTitleEntry("Xian Wang de Richang Shenghuo", "The Daily Life of the Immortal King", listOf("Daily Life of the Immortal King", "Immortal King"), true),
        DesktopTitleEntry("Quan Zhi Gao Shou", "The King's Avatar", listOf("King's Avatar", "Full-Time Expert"), true),
        DesktopTitleEntry("Quanzhi Gaoshou", "The King's Avatar", listOf("King's Avatar"), true),
        DesktopTitleEntry("Quanzhi Fashi", "Full-Time Magister", listOf("Versatile Mage"), true),
        DesktopTitleEntry("Quan Zhi Fa Shi", "Full-Time Magister", listOf("Versatile Mage"), true),
        DesktopTitleEntry("Cike Wu Liuqi", "Scissor Seven", listOf("Killer Seven"), true),
        DesktopTitleEntry("Wu Liuqi", "Scissor Seven", listOf("Killer Seven"), true),
        DesktopTitleEntry("Wu Shan Wu Xing", "Fog Hill of Five Elements", listOf("Fog Hill of the Five Elements"), true),
        DesktopTitleEntry("Zhan Shen", "Slay the Gods", listOf("I Learned to Slay Gods in a Mental Hospital", "God Slayer"), true),
        DesktopTitleEntry("Huo Feng Liao Yuan", "The Ravages of Time", listOf("Ravages of Time"), true),
        DesktopTitleEntry("Juan Siliang", "The Island of Siliang", listOf("Island of Siliang"), true),
        DesktopTitleEntry("Si Liang Dao", "The Island of Siliang", listOf("Island of Siliang"), true),
        DesktopTitleEntry("Xian Wu Chuan", "Legend of Xianwu", listOf("Legend of Martial Immortal", "Xianwu Emperor"), true),
        DesktopTitleEntry("Xian Wu Di Zun", "Legend of Xianwu", listOf("Xianwu Emperor", "Immortal Martial Emperor"), true),
        DesktopTitleEntry("Yuan Zun", "Dragon Prince Yuan", listOf("Dragon Prince Yuan"), true),
        DesktopTitleEntry("Sheng Xu", "The Sacred Ruins", listOf("Sacred Ruins"), true),
        DesktopTitleEntry("Hua Jianghu: Bu Liang Ren", "A Portrait of Jianghu: The Degenerate", listOf("The Degenerate Drawing Jianghu", "The Degenerate"), true),
        DesktopTitleEntry("Bu Liang Ren", "The Degenerate: Drawing Jianghu", listOf("The Degenerate", "Bad Guys"), true),
        DesktopTitleEntry("Da Wang Rao Ming", "Spare Me, Great Lord!", listOf("Spare Me Great Lord"), true),
        DesktopTitleEntry("Yi Ren Zhi Xia", "The Outcast", listOf("Hitori no Shita: The Outcast", "Under One Person"), true),
        DesktopTitleEntry("Zhen Hun Jie", "Rakshasa Street", listOf("Requiem Street"), true),
        DesktopTitleEntry("Ling Long: Incarnation", "Ling Cage: Incarnation", listOf("Spirit Cage: Incarnation", "Ling Cage"), true),
        DesktopTitleEntry("Ling Long", "Ling Cage: Incarnation", listOf("Spirit Cage"), true),
        DesktopTitleEntry("Wan Jie Duzun", "Ten Thousand Worlds", listOf("Lord of Ten Thousand Worlds"), true),
        DesktopTitleEntry("Wan Jie Xian Zong", "Wonderland of Ten Thousand Deities", listOf("Wonderland"), true),
        DesktopTitleEntry("Wan Jie Fa Shen", "God of All Realms", listOf("Law God of Ten Thousand Worlds"), true),
        DesktopTitleEntry("Wan Jie Shen Zhu", "Lord of the Universe", listOf("God of Ten Thousand Worlds"), true),
        DesktopTitleEntry("Ling Jian Zun", "Spirit Sword Sovereign", listOf("Sword Spirit Sovereign"), true),
        DesktopTitleEntry("Du Bu Xiao Yao", "One Step Toward Freedom", listOf("Step to Freedom"), true),
        DesktopTitleEntry("Shen Wu Tian Zun", "Supreme Martial God", listOf("Legend of the Supreme Soldier"), true),
        DesktopTitleEntry("Dan Dao Zhi Zun", "Alchemy Supreme", listOf("Supreme Alchemist"), true),
        DesktopTitleEntry("Jue Shi Wu Hun", "Peerless Battle Spirit", listOf("Peerless Martial Spirit"), true),
        DesktopTitleEntry("Jue Shi Wu Shen", "Peerless Martial God", listOf("Peerless Martial God"), true),
        DesktopTitleEntry("Shen Mu", "Tomb of Fallen Gods", listOf("Divine Tomb"), true),
        DesktopTitleEntry("Long She Yan Yi", "Dragon's Disciple", listOf("Dragon Snake Evolution"), true),
        DesktopTitleEntry("Chao Neng Lifang", "Super Cube", listOf("Super Cube Donghua"), true),
        DesktopTitleEntry("Tu Bian Yingxiong X", "To Be Hero X", listOf("To Be Hero X"), true),
        DesktopTitleEntry("Tu Bian Yingxiong", "To Be Hero", listOf("To Be Hero"), true),
        DesktopTitleEntry("Shi Kun", "All Devouring Whale: Homecoming", listOf("All Devouring Whale"), true),
        DesktopTitleEntry("Kun Tun Tianxia", "All Devouring Whale: Homecoming", listOf("All Devouring Whale"), true),
        DesktopTitleEntry("Tian Xing Jiu Ge", "Nine Songs of the Moving Heavens", listOf("Nine Songs"), true),
        DesktopTitleEntry("Qin Shi Ming Yue", "The Legend of Qin", listOf("Qin's Moon"), true),
        DesktopTitleEntry("Wu Geng Ji", "The Legend and the Hero", listOf("Chronicles of the God's Order"), true),
        DesktopTitleEntry("Xi Xing Ji", "The Westward", listOf("Journey to the Westward"), true),
        DesktopTitleEntry("Shao Nian Ge Xing", "Great Journey of Teenagers", listOf("The Blood of Youth", "Youths and Golden Coffin"), true),
        DesktopTitleEntry("Shao Nian Bai Ma Zui Chun Feng", "Young Brewmaster's Adventure", listOf("Dashing Youth"), true),
        DesktopTitleEntry("An He Chuan", "Tales of Dark River", listOf("Blood River"), true),
        DesktopTitleEntry("Cang Lan Jue", "Love Between Fairy and Devil", listOf("Love Between Fairy and Devil"), true),
        DesktopTitleEntry("Cang Lan Zhi Jue", "Love Between Fairy and Devil", listOf("Love Between Fairy and Devil"), true),
        DesktopTitleEntry("Shan He Jian Xin", "Thousand Autumns", listOf("Thousand Autumns"), true),
        DesktopTitleEntry("Chuan Shu Zijiu Zhinan", "Scumbag System", listOf("The Scum Villain's Self-Saving System"), true),
        DesktopTitleEntry("Tian Bao Fuyao Lu", "Legend of Exorcism", listOf("Legend of Exorcism"), true),
        DesktopTitleEntry("Dinghai Fusheng Lu", "Dinghai Fusheng Records", listOf("Epic of Divinity Light"), true),
        DesktopTitleEntry("Liang Bu Yi", "No Doubt in Us", listOf("No Doubt in Us"), true),
        DesktopTitleEntry("You Shou Yan", "Fabulous Beasts", listOf("Fabulous Beasts"), true),
        DesktopTitleEntry("Fei Ren Zai", "Non-Human", listOf("Fabulous Beasts"), true),
        DesktopTitleEntry("Luo Xiaohei Zhan Ji", "The Legend of Hei", listOf("The Legend of Luo Xiaohei"), true),
        DesktopTitleEntry("Bai She: Yuan Qi", "White Snake", listOf("White Snake Origin"), true),
        DesktopTitleEntry("Qing She: Jie Qi", "Green Snake", listOf("White Snake 2: Green Snake"), true),
        DesktopTitleEntry("Da Yu Hai Tang", "Big Fish & Begonia", listOf("Big Fish and Begonia"), true),
        DesktopTitleEntry("Nezha: Mo Tong Jiang Shi", "Ne Zha", listOf("Nezha: Birth of the Demon Child"), true),
        DesktopTitleEntry("Jiang Ziya", "Jiang Ziya: Legend of Deification", listOf("Legend of Deification"), true),
        DesktopTitleEntry("Hu Yao Xiao Hongniang", "Fox Spirit Matchmaker", listOf("Fox Spirit Matchmaker"), true),
        DesktopTitleEntry("Bing Huo Mo Chu", "The Magic Chef of Ice and Fire", listOf("Magic Chef of Ice and Fire"), true),
        DesktopTitleEntry("Xue Ying Ling Zhu", "Snow Eagle Lord", listOf("Lord Xue Ying"), true),
        DesktopTitleEntry("Yong Sheng", "Immortality", listOf("Eternal Life"), true),
        DesktopTitleEntry("Xing Wu Shen Jue", "Star Martial God Technique", listOf("Star Martial God"), true),
        DesktopTitleEntry("Wan Gu Jian Shen", "Eternal Sword God", listOf("Ten Thousand Ancient Sword God"), true),
        DesktopTitleEntry("Wan Gu Shen Hua", "My Heroic Husband", listOf("Eternal Myth"), true),
        DesktopTitleEntry("Jian Yu Feng Yun", "Sword Domain", listOf("The Wind and Cloud of Sword Domain"), true),
        DesktopTitleEntry("Shen Lan Qi Yu Wushuang Zhu", "The Land of Miracles", listOf("Land of Miracles"), true),
        DesktopTitleEntry("Chang Sheng Jie", "World of Immortals", listOf("Immortal World"), true),
        DesktopTitleEntry("Yuan Long", "Carp Reborn", listOf("First Dragon"), true),
        DesktopTitleEntry("Wu Lian Dian Feng", "Martial Peak", listOf("Peak of Martial Arts"), true),
        DesktopTitleEntry("Zhen Dao Ge", "Song of the Real Blade", listOf("True Blade Song"), true),
        DesktopTitleEntry("Hu He Yao Shi Lu", "Tiger Crane", listOf("Record of Tiger and Crane"), true),
        DesktopTitleEntry("Shi Fang Wu Sheng", "Ten Directions Martial Saint", listOf("Martial Saint of Ten Directions"), true),
        DesktopTitleEntry("Zhi Zun Shen Ming", "Supreme God Emperor", listOf("Supreme God"), true),
        DesktopTitleEntry("Tian Huang Zhan Shen", "Desolate God of War", listOf("God of War"), true),
        DesktopTitleEntry("Zui Qiang Fan Pai", "Strongest Anti M.E.T.A", listOf("Ultimate Scheming System"), true),
        DesktopTitleEntry("Zui Hou De Zhao Huan Shi", "The Last Summoner", listOf("Last Summoner"), true),
        DesktopTitleEntry("Tong Ling Fei", "Psychic Princess", listOf("Psychic Princess"), true),
        DesktopTitleEntry("Xiao Lu He Xiao Lan", "Beryl and Sapphire", listOf("Beryl and Sapphire"), true),
        DesktopTitleEntry("Guan Hai Ce", "Watching the Sea", listOf("Strategies of Watching the Sea"), true),
        DesktopTitleEntry("Feng Shen Bang", "New Gods: Yang Jian", listOf("Investiture of the Gods"), true),
        DesktopTitleEntry("Tai Yi Jian Xian Chuan", "Legend of Taiyi Sword Immortal", listOf("Taiyi Sword Immortal"), true),
        DesktopTitleEntry("Wu Ying Sanqian Dao", "Three Thousand Paths of Martial Arts", listOf("Three Thousand Martial Paths"), true),
        DesktopTitleEntry("Jiu Tian Xuan Di Jue", "Nine Heavens Profound Emperor", listOf("Nine Star Hegemon"), true),
        DesktopTitleEntry("Zhen Wu Dian Feng", "True Martial World", listOf("Peak of True Martial Arts"), true),
        DesktopTitleEntry("Wan Gu Zui Qiang Zong", "The All-Devouring Sect", listOf("Strongest Sect of All Times"), true),
        DesktopTitleEntry("Da Dao Chao Tian", "The Path Toward Heaven", listOf("Path Toward Heaven"), true),
        DesktopTitleEntry("Shen Lan Zhi Yue", "Deep Blue Moon", listOf("Deep Blue Moon"), true),
        DesktopTitleEntry("Xinghai Qihang", "Starry Journey", listOf("Starry Journey"), true),
        DesktopTitleEntry("Bi Lan Zhi Yu", "Azure Legacy", listOf("Azure Legacy"), true),
        DesktopTitleEntry("Jiyi Guanli Ju", "False Memory", listOf("Memory Management Bureau"), true),
        DesktopTitleEntry("Bai Ri Cheng Wang", "Crowned in a Hundred Days", listOf("Crowned in a Hundred Days"), true),
        DesktopTitleEntry("Shi Yi Ci Yuan", "Eleventh Dimension", listOf("Eleventh Dimension"), true),
        DesktopTitleEntry("Lu Shidai", "Lu's Time", listOf("Lu's Time"), true),
        DesktopTitleEntry("Xian Zun Nai Ba Dang Dao", "Immortal Sovereign Dad", listOf("Immortal Dad"), true),
        DesktopTitleEntry("Wo De Shi Xiong Tai Wen Jian Le", "My Senior Brother Is Too Steady", listOf("Big Brother"), true),
        DesktopTitleEntry("Tun Tian Ji", "Heaven Swallowing Record", listOf("Swallowing the Heavens"), true),
        DesktopTitleEntry("Chao Shen Xue Yuan", "Transcend the Gods", listOf("Super God Academy"), true),
        DesktopTitleEntry("Xiong Bing Lian", "The Black Troop", listOf("Transcend the Gods: The Black Troop"), true),
        DesktopTitleEntry("Bai Yao Pu", "Fairies Albums", listOf("Manual of Hundred Demons"), true),
        DesktopTitleEntry("Da Li Si Ri Zhi", "White Cat Legend", listOf("Journal of Dali Temple"), true),
        DesktopTitleEntry("Huan Chong Shi", "The Monster Pet Evolution", listOf("Monster Pet Evolution"), true),

        // Major Japanese Anime (Romaji -> Official English)
        DesktopTitleEntry("Tensei Shitara Slime Datta Ken", "That Time I Got Reincarnated as a Slime", listOf("Tensura", "Slime Isekai")),
        DesktopTitleEntry("Mairimashita! Iruma-kun", "Welcome to Demon School! Iruma-kun", listOf("Iruma-kun")),
        DesktopTitleEntry("Re:Zero kara Hajimeru Isekai Seikatsu", "Re:ZERO -Starting Life in Another World-", listOf("Re:Zero", "Starting Life in Another World")),
        DesktopTitleEntry("Mushoku Tensei: Isekai Ittara Honki Dasu", "Mushoku Tensei: Jobless Reincarnation", listOf("Jobless Reincarnation")),
        DesktopTitleEntry("Kimetsu no Yaiba", "Demon Slayer: Kimetsu no Yaiba", listOf("Demon Slayer")),
        DesktopTitleEntry("Shingeki no Kyojin", "Attack on Titan", listOf("AOT")),
        DesktopTitleEntry("Boku no Hero Academia", "My Hero Academia", listOf("MHA")),
        DesktopTitleEntry("Sousou no Frieren", "Frieren: Beyond Journey's End", listOf("Frieren")),
        DesktopTitleEntry("Kusuriya no Hitorigoto", "The Apothecary Diaries", listOf("Apothecary Diaries")),
        DesktopTitleEntry("Kage no Jitsuryokusha ni Naritakute!", "The Eminence in Shadow", listOf("Eminence in Shadow")),
        DesktopTitleEntry("Tate no Yuusha no Nariagari", "The Rising of the Shield Hero", listOf("Shield Hero")),
        DesktopTitleEntry("Kono Subarashii Sekai ni Shukufuku o!", "KONOSUBA -God's blessing on this wonderful world!", listOf("Konosuba")),
        DesktopTitleEntry("Kaguya-sama wa Kokurasetai", "Kaguya-sama: Love is War", listOf("Love is War")),
        DesktopTitleEntry("Youjo Senki", "Saga of Tanya the Evil", listOf("Tanya the Evil")),
        DesktopTitleEntry("BLEACH: Sennen Kessen-hen", "BLEACH: Thousand-Year Blood War", listOf("Bleach TYBW")),
        DesktopTitleEntry("Ore dake Level Up na Ken", "Solo Leveling", listOf("Solo Leveling")),
        DesktopTitleEntry("Meitantei Conan", "Detective Conan", listOf("Case Closed")),
        DesktopTitleEntry("Kaijuu 8-gou", "Kaiju No. 8", listOf("Kaiju 8")),
        DesktopTitleEntry("Dungeon Meshi", "Delicious in Dungeon", listOf("Delicious in Dungeon")),
        DesktopTitleEntry("Boku no Kokoro no Yabai Yatsu", "The Dangers in My Heart", listOf("Dangers in My Heart")),
        DesktopTitleEntry("Yofukashi no Uta", "Call of the Night", listOf("Call of the Night")),
        DesktopTitleEntry("Youkoso Jitsuryoku Shijou Shugi no Kyoushitsu e", "Classroom of the Elite", listOf("COTE")),
        DesktopTitleEntry("Kami no Tou", "Tower of God", listOf("Tower of God")),
        DesktopTitleEntry("Hagane no Renkinjutsushi", "Fullmetal Alchemist", listOf("FMA", "Fullmetal Alchemist: Brotherhood")),
        DesktopTitleEntry("Enen no Shouboutai", "Fire Force", listOf("Fire Force")),
        DesktopTitleEntry("Nanatsu no Taizai", "The Seven Deadly Sins", listOf("Seven Deadly Sins")),
        DesktopTitleEntry("Yakusoku no Neverland", "The Promised Neverland", listOf("Promised Neverland")),
        DesktopTitleEntry("Ansatsu Kyoushitsu", "Assassination Classroom", listOf("Assassination Classroom")),
        DesktopTitleEntry("Seishun Buta Yarou wa Bunny Girl Senpai no Yume wo Minai", "Rascal Does Not Dream of Bunny Girl Senpai", listOf("Bunny Girl Senpai")),
        DesktopTitleEntry("Dungeon ni Deai wo Motomeru no wa Machigatteiru Darou ka", "Is It Wrong to Try to Pick Up Girls in a Dungeon?", listOf("DanMachi")),
        DesktopTitleEntry("Shigatsu wa Kimi no Uso", "Your Lie in April", listOf("Your Lie in April")),
        DesktopTitleEntry("Koe no Katachi", "A Silent Voice", listOf("Silent Voice")),
        DesktopTitleEntry("Kimi no Na wa.", "Your Name.", listOf("Your Name")),
        DesktopTitleEntry("Tenki no Ko", "Weathering With You", listOf("Weathering With You")),
        DesktopTitleEntry("Sen to Chihiro no Kamikakushi", "Spirited Away", listOf("Spirited Away")),
        DesktopTitleEntry("Mononoke Hime", "Princess Mononoke", listOf("Princess Mononoke")),
        DesktopTitleEntry("Howl no Ugoku Shiro", "Howl's Moving Castle", listOf("Howl's Moving Castle"))
    )

    private val normalizedMap: Map<String, DesktopTitleEntry> by lazy {
        val map = LinkedHashMap<String, DesktopTitleEntry>()
        for (entry in entries) {
            val key = normalizeKey(entry.pinyinOrRomaji)
            if (key.isNotEmpty() && !map.containsKey(key)) map[key] = entry
            val compactKey = key.replace(" ", "")
            if (compactKey.length >= 6 && !map.containsKey(compactKey)) map[compactKey] = entry
        }
        map
    }

    private val sortedPrefixEntries: List<Pair<String, DesktopTitleEntry>> by lazy {
        entries.map { normalizeKey(it.pinyinOrRomaji) to it }
            .filter { it.first.length >= 5 }
            .sortedByDescending { it.first.length }
    }

    private val patternReplacements = listOf(
        Regex("(?i)\\b4th Season\\b") to "Season 4",
        Regex("(?i)\\b3rd Season\\b") to "Season 3",
        Regex("(?i)\\b2nd Season\\b") to "Season 2",
        Regex("(?i)\\b1st Season\\b") to "Season 1",
        Regex("(?i)\\bDai\\s*(\\d+)\\s*Ki\\b") to "Season $1",
        Regex("(?i)\\bDi\\s*Er\\s*Ji\\b") to "Season 2",
        Regex("(?i)\\bDi\\s*San\\s*Ji\\b") to "Season 3",
        Regex("(?i)\\bDi\\s*Si\\s*Ji\\b") to "Season 4",
        Regex("(?i)\\bDi\\s*Wu\\s*Ji\\b") to "Season 5",
        Regex("(?i)\\bDi\\s*Liu\\s*Ji\\b") to "Season 6",
        Regex("(?i)\\bDi\\s*(\\d+)\\s*Ji\\b") to "Season $1",
        Regex("(?i)\\bNian\\s*Fan\\b") to "Annual Edition",
        Regex("(?i)\\bTebie\\s*Pian\\b") to "Special",
        Regex("(?i)\\bFanwai\\b") to "Special",
        Regex("(?i)\\bQianzhuan\\b") to "Prequel",
        Regex("(?i)\\bJuchang\\s*Ban\\b") to "Movie"
    )

    private val ENGLISH_CORE_WORDS = setOf(
        "the", "of", "in", "to", "and", "for", "with", "from", "by", "on", "at", "as", "my", "our", "your",
        "world", "god", "gods", "immortal", "immortality", "eternal", "eternity", "heaven", "heavens", "heavenly",
        "sky", "star", "stars", "starry", "soul", "spirit", "martial", "sword", "blade", "master", "lord", "king",
        "emperor", "prince", "princess", "queen", "demon", "demons", "devil", "angel", "dragon", "phoenix", "tiger",
        "wolf", "beast", "beasts", "monster", "hero", "legend", "legends", "tales", "tale", "story", "chronicles",
        "record", "records", "journey", "adventure", "quest", "battle", "war", "fight", "fights", "slay", "slayer",
        "hunter", "assassin", "killer", "shadow", "dark", "light", "blood", "fire", "ice", "wind", "thunder",
        "earth", "sea", "river", "mountain", "island", "land", "realm", "realms", "universe", "cosmos", "space",
        "time", "life", "death", "dead", "tomb", "grave", "ruins", "sacred", "divine", "holy", "supreme", "sovereign",
        "peerless", "unrivaled", "great", "grand", "ancient", "mortal", "renegade", "against", "through", "beyond",
        "swallowed", "devouring", "shrouding", "herding", "coming", "throne", "seal", "sealed", "dynasty", "jade",
        "cultivation", "alchemy", "refining", "sect", "clan", "school", "academy", "brother", "sister", "senior",
        "steady", "perfect", "stellar", "transformations", "apotheosis", "mysteries", "mystery", "magic", "magister",
        "mage", "summoner", "outcast", "degenerate", "street", "cage", "incarnation", "click", "link", "blessing",
        "official", "avatar", "scissor", "seven", "fog", "hill", "five", "elements", "ravages", "spare", "me",
        "daily", "will", "thousand", "ten", "hundred", "years", "step", "freedom", "disciple", "super", "cube",
        "whale", "songs", "youth", "love", "between", "fairy", "autumns", "system", "exorcism", "doubt", "fabulous",
        "snake", "white", "green", "black", "red", "blue", "gold", "golden", "silver", "big", "fish", "begonia",
        "fox", "matchmaker", "chef", "snow", "eagle", "peak", "domain", "miracles", "carp", "reborn", "path",
        "toward", "season", "part", "movie", "special", "edition", "reincarnated", "slime", "titan", "attack",
        "apothecary", "diaries", "shield", "eminence", "classroom", "elite", "tower", "solo", "leveling", "online",
        "piece", "punch", "note", "detective", "silent", "voice", "name", "weathering", "spirited", "away",
        "moving", "castle", "promised", "neverland", "deadly", "sins", "clover", "stone", "force"
    )

    private val PINYIN_MARKERS = setOf(
        "zh", "ch", "sh", "xi", "qi", "ji", "yu", "lv", "xue", "zhe", "tian", "xian", "shen", "dou", "luo",
        "wang", "zuo", "cang", "qiong", "ling", "zun", "jian", "wan", "jie", "wu", "dao", "mo", "feng", "yun",
        "long", "huo", "bing", "lei", "xing", "kong", "tun", "shi", "ren", "chuan", "zhuan", "zhi", "bu", "nian",
        "fan", "bai", "hei", "qing", "hong", "jin", "yin", "yuan", "mu", "gu", "shan", "he", "xiao", "da", "tai",
        "jue", "yao", "shijie", "dalu", "xingkong", "cangqiong", "xiuxian", "wangzuo", "dailiren", "gaoshou",
        "fashi", "liuqi", "liang", "jianghu", "siliang", "yongheng", "qiankun", "zhan", "gong", "cheng", "ming",
        "shuang", "huan", "chong", "shao", "chun", "qiu", "dong", "xia", "dizi", "zong", "men"
    )

    private val NON_ENGLISH_LATIN_MARKERS = setOf(
        "el", "la", "los", "las", "del", "una", "con", "para", "por", "der", "die", "das", "und", "ein", "eine",
        "les", "des", "une", "dans", "pour", "avec", "cua", "nguoi", "dau", "pha", "gioi", "tinh", "tien"
    )

    private val JAPANESE_GRAMMAR_TOKENS = setOf(
        "no", "wa", "ga", "wo", "ni", "kara", "made", "datta", "ken", "naritakute", "shitara", "desu", "deshita",
        "monogatari", "sekai", "isekai", "yuusha", "maou", "mahou", "shoujo", "shounen", "gakuen", "koukou",
        "kyoushitsu", "senpai", "sensei", "tachi", "tensei", "akuyaku", "reijou"
    )

    fun normalizeKey(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val decomposed = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return decomposed
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private fun compactAlphaNum(raw: String?): String = normalizeKey(raw).replace(" ", "")

    private fun hasNonLatinScript(text: String): Boolean {
        return text.any { ch ->
            val code = ch.code
            (code in 0x0400..0x052F) ||
                (code in 0x0590..0x06FF) ||
                (code in 0x0E00..0x0E7F) ||
                (code in 0x0900..0x097F) ||
                (code in 0x3040..0x30FF) ||
                (code in 0x3400..0x9FFF) ||
                (code in 0xAC00..0xD7AF) ||
                (code in 0xF900..0xFAFF)
        }
    }

    private fun hasNonEnglishDiacritics(text: String): Boolean {
        return text.any { ch ->
            val code = ch.code
            (code in 0x1E00..0x1EFF) ||
                ch in setOf('đ', 'Đ', 'ă', 'Ă', 'ơ', 'Ơ', 'ư', 'Ư', 'ñ', 'Ñ', 'ß', 'ç', 'Ç', 'ł', 'Ł', 'ż', 'Ż', 'ą', 'Ą', 'ę', 'Ę')
        }
    }

    fun isLikelyPinyin(candidate: String, romaji: String? = null): Boolean {
        val trimmed = candidate.trim()
        if (trimmed.isEmpty() || hasNonLatinScript(trimmed)) return false

        val normCand = normalizeKey(trimmed)
        val compactCand = normCand.replace(" ", "")
        val compactRom = compactAlphaNum(romaji)

        if (compactRom.length >= 4 && (compactCand == compactRom || (compactCand.startsWith(compactRom) && compactCand.length - compactRom.length <= 4))) {
            val tokens = normCand.split(" ").filter { it.isNotEmpty() }
            if (tokens.none { it in ENGLISH_CORE_WORDS && it.length >= 3 }) return true
        }

        if (normalizedMap[normCand]?.isDonghua == true || normalizedMap[compactCand]?.isDonghua == true) {
            return true
        }

        val tokens = normCand.split(" ").filter { it.isNotEmpty() && !it.all(Char::isDigit) }
        if (tokens.isEmpty()) return false

        val englishHits = tokens.count { it in ENGLISH_CORE_WORDS }
        if (englishHits >= 2 || (englishHits == 1 && tokens.size <= 2)) return false

        var pinyinTokenScore = 0
        for (tok in tokens) {
            if (tok in PINYIN_MARKERS) pinyinTokenScore += 2
            else if (Regex("^(zh|ch|sh|x|q|z|c)[aeiou][a-z]{0,4}$").matches(tok)) pinyinTokenScore += 1
            else if (Regex("^[a-z]*(ang|eng|ing|ong|uan|ian|iao|iang|iong|ue|uo|ou|ai|ei)$").matches(tok) && tok !in ENGLISH_CORE_WORDS) {
                pinyinTokenScore += 1
            }
        }
        return pinyinTokenScore >= maxOf(2, tokens.size)
    }

    private fun scoreEnglishSynonym(candidate: String, romaji: String?, isDonghua: Boolean): Int {
        val clean = candidate.trim()
        if (clean.length < 3 || clean.length > 120) return -100
        if (hasNonLatinScript(clean)) return -100
        if (hasNonEnglishDiacritics(clean)) return -80

        val norm = normalizeKey(clean)
        if (norm.isEmpty()) return -100
        val compact = norm.replace(" ", "")
        val compactRom = compactAlphaNum(romaji)

        val lettersOnly = clean.filter { it.isLetter() }
        if (lettersOnly.isEmpty()) return -100
        if (lettersOnly.length <= 5 && lettersOnly.all { it.isUpperCase() } && !clean.contains(" ")) return -60

        if (compactRom.length >= 4 && compact == compactRom) return -50
        if (isDonghua && isLikelyPinyin(clean, romaji)) return -70

        val tokens = norm.split(" ").filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return -100

        val englishWordHits = tokens.count { it in ENGLISH_CORE_WORDS }
        val jpHits = tokens.count { it in JAPANESE_GRAMMAR_TOKENS }
        if (jpHits >= 2 || (jpHits >= 1 && englishWordHits == 0)) return -50

        val foreignHits = tokens.count { it in NON_ENGLISH_LATIN_MARKERS }
        if (foreignHits >= 1 && englishWordHits == 0) return -45

        if (entries.any { it.englishTitle.equals(clean, ignoreCase = true) || it.alternateEnglish.any { alt -> alt.equals(clean, ignoreCase = true) } }) {
            return 200
        }

        var score = 0
        if (englishWordHits > 0) {
            score += 40 + (englishWordHits * 18)
        } else if (isDonghua) {
            val looksAscii = clean.all { it.code < 128 }
            if (looksAscii && !isLikelyPinyin(clean, romaji) && tokens.size in 1..10) {
                score += 18
            } else {
                return -20
            }
        } else {
            return -10
        }

        if (tokens.size in 2..9) score += 12
        if (clean.length in 6..75) score += 8
        if (clean.contains("(") && clean.contains(")")) score -= 6
        if (clean.all { it.code < 128 }) score += 6
        return score
    }

    private fun pickBestEnglishSynonym(synonyms: List<String>?, romaji: String?, isDonghua: Boolean): String? {
        if (synonyms.isNullOrEmpty()) return null
        var best: String? = null
        var bestScore = 0
        for (syn in synonyms) {
            val clean = syn.trim()
            if (clean.isEmpty()) continue
            val sc = scoreEnglishSynonym(clean, romaji, isDonghua)
            if (sc > bestScore) {
                bestScore = sc
                best = clean
            }
        }
        return best
    }

    private fun extractTrailingSuffix(raw: String, matchedPrefixNorm: String): String {
        val normFull = normalizeKey(raw)
        if (!normFull.startsWith(matchedPrefixNorm) || normFull.length == matchedPrefixNorm.length) return ""
        val remainder = normFull.removePrefix(matchedPrefixNorm).trim()
        if (remainder.isEmpty()) return ""

        Regex("^(?:season\\s*)?(\\d+)$").matchEntire(remainder)?.let { m ->
            val num = m.groupValues[1].toIntOrNull()
            if (num != null && num in 2..20) return "Season $num"
        }
        Regex("^(ii|iii|iv|v|vi|vii|viii|ix|x)$").matchEntire(remainder)?.let { m ->
            val num = when (m.groupValues[1]) {
                "ii" -> 2; "iii" -> 3; "iv" -> 4; "v" -> 5; "vi" -> 6
                "vii" -> 7; "viii" -> 8; "ix" -> 9; "x" -> 10
                else -> null
            }
            if (num != null) return "Season $num"
        }
        Regex("^(\\d+)(?:nd|rd|th)\\s+season$").matchEntire(remainder)?.let { m ->
            return "Season ${m.groupValues[1]}"
        }
        Regex("^di\\s*(\\d+|er|san|si|wu|liu|qi|ba)\\s*ji$").matchEntire(remainder)?.let { m ->
            val num = when (m.groupValues[1]) {
                "er" -> "2"; "san" -> "3"; "si" -> "4"; "wu" -> "5"; "liu" -> "6"; "qi" -> "7"; "ba" -> "8"
                else -> m.groupValues[1]
            }
            return "Season $num"
        }
        if (remainder.contains("nian fan")) return "Annual Edition"
        if (remainder.contains("movie") || remainder.contains("juchang")) return "Movie"
        if (remainder.contains("special") || remainder.contains("tebie") || remainder.contains("fanwai")) return "Special"
        if (remainder.contains("qianzhuan") || remainder.contains("prequel")) return "Prequel"
        Regex("^part\\s*(\\d+)$").matchEntire(remainder)?.let { m -> return "Part ${m.groupValues[1]}" }
        return ""
    }

    private fun findInTitleMap(query: String): String? {
        val clean = query.trim()
        if (clean.isEmpty()) return null
        val norm = normalizeKey(clean)
        val compact = norm.replace(" ", "")

        normalizedMap[norm]?.let { return it.englishTitle }
        normalizedMap[compact]?.let { return it.englishTitle }

        for ((prefixNorm, entry) in sortedPrefixEntries) {
            if (norm == prefixNorm || norm.startsWith("$prefixNorm ")) {
                val suffix = extractTrailingSuffix(clean, prefixNorm)
                return if (suffix.isNotEmpty() && !entry.englishTitle.contains(suffix, ignoreCase = true)) {
                    "${entry.englishTitle} $suffix"
                } else {
                    entry.englishTitle
                }
            }
        }
        return null
    }

    fun resolve(
        titleEnglish: String?,
        titleRomaji: String?,
        synonyms: List<String>? = null,
        isDonghua: Boolean = false
    ): String? {
        val eng = titleEnglish?.trim()?.takeIf { it.isNotEmpty() }
        val rom = titleRomaji?.trim()?.takeIf { it.isNotEmpty() }

        val effectiveDonghua = isDonghua ||
            (rom != null && normalizedMap[normalizeKey(rom)]?.isDonghua == true) ||
            (eng != null && normalizedMap[normalizeKey(eng)]?.isDonghua == true)

        if (eng != null) {
            val mappedEng = findInTitleMap(eng)
            if (mappedEng != null) return mappedEng
            val engIsPinyin = effectiveDonghua && isLikelyPinyin(eng, rom)
            if (!engIsPinyin && !hasNonLatinScript(eng)) return applyPatterns(eng)
        }

        if (rom != null) {
            val mappedRom = findInTitleMap(rom)
            if (mappedRom != null) return mappedRom
        }

        synonyms?.forEach { syn ->
            val mapped = findInTitleMap(syn)
            if (mapped != null) return mapped
        }

        val bestSyn = pickBestEnglishSynonym(synonyms, rom, effectiveDonghua)
        if (bestSyn != null) return applyPatterns(bestSyn)

        if (rom != null) {
            val trans = applyPatterns(rom)
            if (trans != rom) return trans
        }

        if (eng != null) return applyPatterns(eng)
        return null
    }

    private val SEARCH_STOP_WORDS = setOf(
        "of", "the", "a", "an", "in", "to", "and", "or", "for", "on", "at", "by", "as", "is", "it", "my"
    )

    fun expandSearchQueries(rawQuery: String, donghuaOnly: Boolean = false, maxExtra: Int = 2): List<String> {
        val normQuery = normalizeKey(rawQuery)
        if (normQuery.length < 2) return emptyList()
        val rawTokens = normQuery.split(" ").filter { it.length >= 2 }
        if (rawTokens.isEmpty()) return emptyList()
        val contentTokens = rawTokens.filter { it !in SEARCH_STOP_WORDS }
        val effectiveTokens = if (contentTokens.isNotEmpty()) contentTokens else rawTokens

        val scored = ArrayList<Pair<String, Int>>()
        val seen = HashSet<String>()
        for (entry in entries) {
            if (donghuaOnly && !entry.isDonghua) continue
            val pinyinNorm = normalizeKey(entry.pinyinOrRomaji)
            if (pinyinNorm == normQuery || !seen.add(pinyinNorm)) continue

            val candidates = listOf(entry.englishTitle) + entry.alternateEnglish
            var best = 0
            for (c in candidates) {
                val candNorm = normalizeKey(c)
                if (contentTokens.isNotEmpty() && contentTokens.none { tok -> candNorm.contains(tok) }) continue
                val sc = scoreSingleTextMatch(normQuery, effectiveTokens, c)
                if (sc > best) best = sc
            }
            if (best >= 40) {
                scored.add(entry.pinyinOrRomaji to (best + if (entry.isDonghua) 15 else 0))
            }
        }

        return scored
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctBy { normalizeKey(it) }
            .take(maxExtra)
    }

    fun scoreKeywordMatch(
        rawQuery: String,
        resolvedEnglish: String?,
        officialEnglish: String?,
        romaji: String?,
        synonyms: List<String>? = null
    ): Int {
        val normQuery = normalizeKey(rawQuery)
        if (normQuery.isEmpty()) return 0
        val queryTokens = normQuery.split(" ").filter { it.isNotEmpty() }
        if (queryTokens.isEmpty()) return 0

        var best = 0
        val fields = buildList {
            if (!resolvedEnglish.isNullOrBlank()) add(resolvedEnglish to 12)
            if (!officialEnglish.isNullOrBlank()) add(officialEnglish to 10)
            if (!romaji.isNullOrBlank()) add(romaji to 6)
            synonyms.orEmpty().forEach { if (it.isNotBlank()) add(it to 8) }
        }
        for ((text, bonus) in fields) {
            val sc = scoreSingleTextMatch(normQuery, queryTokens, text)
            if (sc > 0 && sc + bonus > best) best = sc + bonus
        }
        return best
    }

    fun matchesAllKeywords(rawQuery: String, vararg fields: String?): Boolean {
        val normQuery = normalizeKey(rawQuery)
        if (normQuery.isEmpty()) return true
        val tokens = normQuery.split(" ").filter { it.isNotEmpty() }
        val combined = fields.filterNotNull().joinToString(" ") { normalizeKey(it) }
        if (combined.isEmpty()) return false
        return tokens.all { combined.contains(it) }
    }

    private fun scoreSingleTextMatch(normQuery: String, queryTokens: List<String>, rawText: String): Int {
        val normText = normalizeKey(rawText)
        if (normText.isEmpty()) return 0

        if (normText == normQuery) return 140
        if (normText.startsWith("$normQuery ")) return 120
        if (normText.startsWith(normQuery)) return 110
        if (normText.contains(" $normQuery ")) return 100
        if (normText.contains(normQuery)) return 90

        val textTokens = normText.split(" ").filter { it.isNotEmpty() }
        if (textTokens.isEmpty()) return 0

        var exactWord = 0
        var prefixWord = 0
        var substrWord = 0
        for (qt in queryTokens) {
            when {
                textTokens.any { it == qt } -> exactWord++
                textTokens.any { it.startsWith(qt) } -> prefixWord++
                normText.contains(qt) -> substrWord++
            }
        }

        val total = exactWord + prefixWord + substrWord
        if (total == 0) return 0
        val allMatched = total == queryTokens.size
        return when {
            allMatched && exactWord == queryTokens.size -> 95
            allMatched && (exactWord + prefixWord) == queryTokens.size -> 82
            allMatched -> 70
            else -> (exactWord * 28) + (prefixWord * 20) + (substrWord * 10)
        }
    }

    private fun applyPatterns(text: String): String {
        var result = text.trim()
        for ((regex, replacement) in patternReplacements) {
            result = regex.replace(result, replacement)
        }
        return result.replace(Regex("\\s+"), " ").trim()
    }
}
