package com.example.util

import java.text.Normalizer

data class TitleDictionaryEntry(
    val pinyinOrRomaji: String,
    val englishTitle: String,
    val alternateEnglish: List<String> = emptyList(),
    val isDonghua: Boolean = false
)

object EnglishTitleMapper {

    val entries: List<TitleDictionaryEntry> = listOf(
        // =====================================================================
        // CHINESE DONGHUA (Curated Official & Alternate English Titles)
        // =====================================================================
        TitleDictionaryEntry("Wanmei Shijie", "Perfect World", listOf("Wanmei Shijie", "Perfect World Donghua"), true),
        TitleDictionaryEntry("Xian Ni", "Renegade Immortal", listOf("Xian Ni", "Immortal Renegade", "Wang Lin"), true),
        TitleDictionaryEntry("Tunshi Xingkong", "Swallowed Star", listOf("Swallow the Starry Sky", "Devouring the Starry Sky"), true),
        TitleDictionaryEntry("Douluo Dalu 2: Jueshi Tangmen", "Soul Land 2: The Peerless Tang Clan", listOf("Unrivaled Tang Sect", "Soul Land II"), true),
        TitleDictionaryEntry("Douluo Dalu II", "Soul Land 2: The Peerless Tang Clan", listOf("Soul Land 2", "Peerless Tang Clan"), true),
        TitleDictionaryEntry("Douluo Dalu", "Soul Land", listOf("Combat Continent", "Douluo Continent"), true),
        TitleDictionaryEntry("Dou Po Cang Qiong", "Battle Through the Heavens", listOf("Fights Break Sphere", "Fight Break Sphere", "BTTH"), true),
        TitleDictionaryEntry("Doupo Cangqiong", "Battle Through the Heavens", listOf("Fights Break Sphere", "BTTH"), true),
        TitleDictionaryEntry("Fan Ren Xiu Xian Chuan", "A Record of a Mortal's Journey to Immortality", listOf("Mortal's Journey to Immortality", "Record of a Mortal's Journey to Immortality", "RMJI"), true),
        TitleDictionaryEntry("Fanren Xiuxian Zhuan", "A Record of a Mortal's Journey to Immortality", listOf("Mortal's Journey to Immortality"), true),
        TitleDictionaryEntry("Zhe Tian", "Shrouding the Heavens", listOf("Shroud the Heavens", "Covering the Sky"), true),
        TitleDictionaryEntry("Shen Yin Wang Zuo", "Throne of Seal", listOf("Sealed Divine Throne", "Divine Throne"), true),
        TitleDictionaryEntry("Shenyin Wangzuo", "Throne of Seal", listOf("Sealed Divine Throne"), true),
        TitleDictionaryEntry("Mu Shen Ji", "Tales of Herding Gods", listOf("Herding Gods", "Record of Herding Gods"), true),
        TitleDictionaryEntry("Jian Lai", "Sword of Coming", listOf("Sword Come", "Unsheathed"), true),
        TitleDictionaryEntry("Gui Mi Zhi Zhu", "Lord of the Mysteries", listOf("Lord of Mysteries", "LOTM"), true),
        TitleDictionaryEntry("Guimi Zhi Zhu", "Lord of the Mysteries", listOf("Lord of Mysteries"), true),
        TitleDictionaryEntry("Shi Xiong A Shi Xiong", "Big Brother", listOf("My Senior Brother Is Too Steady", "Senior Brother Is Too Steady"), true),
        TitleDictionaryEntry("Cang Yuan Tu", "The Demon Hunter", listOf("Archean Eon Art", "Cangyuan Tu"), true),
        TitleDictionaryEntry("Bai Lian Cheng Shen", "Apotheosis", listOf("Elevation to the Status of a God", "Refining into a God"), true),
        TitleDictionaryEntry("Wu Shen Zhu Zai", "Martial Master", listOf("Dominant Martial God", "God of Martial Arts"), true),
        TitleDictionaryEntry("Lian Qi Shi Wan Nian", "One Hundred Thousand Years of Qi Refining", listOf("100,000 Years of Qi Refining", "100000 Years of Qi Refining"), true),
        TitleDictionaryEntry("Zhu Xian", "Jade Dynasty", listOf("Legend of Chusen", "Zhu Xian"), true),
        TitleDictionaryEntry("Da Zhu Zai", "The Great Ruler", listOf("Great Ruler"), true),
        TitleDictionaryEntry("Ni Tian Xie Shen", "Against the Gods", listOf("Against the Sky Supreme"), true),
        TitleDictionaryEntry("Ni Tian Zhizun", "Against the Sky Supreme", listOf("Against the Gods", "Supreme Against the Sky"), true),
        TitleDictionaryEntry("Shiguang Dailiren", "Link Click", listOf("Time Agent", "Time Agents"), true),
        TitleDictionaryEntry("Tian Guan Ci Fu", "Heaven Official's Blessing", listOf("Heaven Official Blessing", "TGCF"), true),
        TitleDictionaryEntry("Mo Dao Zu Shi", "The Founder of Diabolism", listOf("Grandmaster of Demonic Cultivation", "The Grandmaster of Demonic Cultivation", "MDZS"), true),
        TitleDictionaryEntry("Yi Nian Yong Heng", "A Will Eternal", listOf("One Thought Eternity", "Will Eternal"), true),
        TitleDictionaryEntry("Wu Dong Qian Kun", "Martial Universe", listOf("Martial Movement Upheaval"), true),
        TitleDictionaryEntry("Xing Chen Bian", "Stellar Transformations", listOf("Stellar Transformation", "Legend of Immortals"), true),
        TitleDictionaryEntry("Yao Shen Ji", "Tales of Demons and Gods", listOf("Demons and Gods"), true),
        TitleDictionaryEntry("Xian Wang de Richang Shenghuo", "The Daily Life of the Immortal King", listOf("Daily Life of the Immortal King", "Immortal King"), true),
        TitleDictionaryEntry("Quan Zhi Gao Shou", "The King's Avatar", listOf("King's Avatar", "Full-Time Expert"), true),
        TitleDictionaryEntry("Quanzhi Gaoshou", "The King's Avatar", listOf("King's Avatar"), true),
        TitleDictionaryEntry("Quanzhi Fashi", "Full-Time Magister", listOf("Versatile Mage"), true),
        TitleDictionaryEntry("Quan Zhi Fa Shi", "Full-Time Magister", listOf("Versatile Mage"), true),
        TitleDictionaryEntry("Cike Wu Liuqi", "Scissor Seven", listOf("Killer Seven", "Wu Liuqi"), true),
        TitleDictionaryEntry("Wu Liuqi", "Scissor Seven", listOf("Killer Seven"), true),
        TitleDictionaryEntry("Cikang Qi", "Scissor Seven", listOf("Killer Seven"), true),
        TitleDictionaryEntry("Wu Shan Wu Xing", "Fog Hill of Five Elements", listOf("Fog Hill of the Five Elements"), true),
        TitleDictionaryEntry("Zhan Shen", "Slay the Gods", listOf("I Learned to Slay Gods in a Mental Hospital", "God Slayer"), true),
        TitleDictionaryEntry("Zhan Shen: Fan Shen", "Slay the Gods", listOf("Slay the Gods"), true),
        TitleDictionaryEntry("Huo Feng Liao Yuan", "The Ravages of Time", listOf("Ravages of Time"), true),
        TitleDictionaryEntry("Juan Siliang", "The Island of Siliang", listOf("Island of Siliang"), true),
        TitleDictionaryEntry("Si Liang Dao", "The Island of Siliang", listOf("Island of Siliang"), true),
        TitleDictionaryEntry("Xian Wu Chuan", "Legend of Xianwu", listOf("Legend of Martial Immortal", "Xianwu Emperor"), true),
        TitleDictionaryEntry("Xian Wu Di Zun", "Legend of Xianwu", listOf("Xianwu Emperor", "Immortal Martial Emperor"), true),
        TitleDictionaryEntry("Yuan Zun", "Dragon Prince Yuan", listOf("Yuan Zun"), true),
        TitleDictionaryEntry("Sheng Xu", "The Sacred Ruins", listOf("Sacred Ruins"), true),
        TitleDictionaryEntry("Hua Jianghu: Bu Liang Ren", "A Portrait of Jianghu: The Degenerate", listOf("The Degenerate Drawing Jianghu", "Bu Liang Ren", "The Degenerate"), true),
        TitleDictionaryEntry("Bu Liang Ren", "The Degenerate: Drawing Jianghu", listOf("The Degenerate", "Bad Guys"), true),
        TitleDictionaryEntry("Da Wang Rao Ming", "Spare Me, Great Lord!", listOf("Spare Me Great Lord"), true),
        TitleDictionaryEntry("Yi Ren Zhi Xia", "The Outcast", listOf("Hitori no Shita: The Outcast", "Under One Person"), true),
        TitleDictionaryEntry("Zhen Hun Jie", "Rakshasa Street", listOf("Requiem Street"), true),
        TitleDictionaryEntry("Ling Long: Incarnation", "Ling Cage: Incarnation", listOf("Spirit Cage: Incarnation", "Ling Cage"), true),
        TitleDictionaryEntry("Ling Long", "Ling Cage: Incarnation", listOf("Spirit Cage"), true),
        TitleDictionaryEntry("Wan Jie Duzun", "Ten Thousand Worlds", listOf("Lord of Ten Thousand Worlds"), true),
        TitleDictionaryEntry("Wan Jie Xian Zong", "Wonderland of Ten Thousand Deities", listOf("Wonderland"), true),
        TitleDictionaryEntry("Wan Jie Fa Shen", "God of All Realms", listOf("Law God of Ten Thousand Worlds"), true),
        TitleDictionaryEntry("Wan Jie Shen Zhu", "Lord of the Universe", listOf("God of Ten Thousand Worlds"), true),
        TitleDictionaryEntry("Ling Jian Zun", "Spirit Sword Sovereign", listOf("Sword Spirit Sovereign"), true),
        TitleDictionaryEntry("Du Bu Xiao Yao", "One Step Toward Freedom", listOf("Step to Freedom"), true),
        TitleDictionaryEntry("Shen Wu Tian Zun", "Supreme Martial God", listOf("Legend of the Supreme Soldier"), true),
        TitleDictionaryEntry("Dan Dao Zhi Zun", "Alchemy Supreme", listOf("Supreme Alchemist"), true),
        TitleDictionaryEntry("Jue Shi Wu Hun", "Peerless Battle Spirit", listOf("Peerless Martial Spirit"), true),
        TitleDictionaryEntry("Jue Shi Wu Shen", "Peerless Martial God", listOf("Peerless Martial God"), true),
        TitleDictionaryEntry("Shen Mu", "Tomb of Fallen Gods", listOf("Divine Tomb"), true),
        TitleDictionaryEntry("Long She Yan Yi", "Dragon's Disciple", listOf("Dragon Snake Evolution"), true),
        TitleDictionaryEntry("Chao Neng Lifang", "Super Cube", listOf("Super Cube Donghua"), true),
        TitleDictionaryEntry("Tu Bian Yingxiong X", "To Be Hero X", listOf("To Be Hero X"), true),
        TitleDictionaryEntry("Tu Bian Yingxiong", "To Be Hero", listOf("To Be Hero"), true),
        TitleDictionaryEntry("Shi Kun", "All Devouring Whale: Homecoming", listOf("All Devouring Whale"), true),
        TitleDictionaryEntry("Kun Tun Tianxia", "All Devouring Whale: Homecoming", listOf("All Devouring Whale"), true),
        TitleDictionaryEntry("Tian Xing Jiu Ge", "Nine Songs of the Moving Heavens", listOf("Nine Songs"), true),
        TitleDictionaryEntry("Qin Shi Ming Yue", "The Legend of Qin", listOf("Qin's Moon"), true),
        TitleDictionaryEntry("Wu Geng Ji", "The Legend and the Hero", listOf("Chronicles of the God's Order"), true),
        TitleDictionaryEntry("Xi Xing Ji", "The Westward", listOf("Journey to the Westward"), true),
        TitleDictionaryEntry("Shao Nian Ge Xing", "Great Journey of Teenagers", listOf("The Blood of Youth", "Youths and Golden Coffin"), true),
        TitleDictionaryEntry("Shao Nian Bai Ma Zui Chun Feng", "Young Brewmaster's Adventure", listOf("Dashing Youth"), true),
        TitleDictionaryEntry("An He Chuan", "Tales of Dark River", listOf("Blood River"), true),
        TitleDictionaryEntry("Cang Lan Jue", "Love Between Fairy and Devil", listOf("Cang Lan Jue"), true),
        TitleDictionaryEntry("Cang Lan Zhi Jue", "Love Between Fairy and Devil", listOf("Love Between Fairy and Devil"), true),
        TitleDictionaryEntry("Shan He Jian Xin", "Thousand Autumns", listOf("Thousand Autumns"), true),
        TitleDictionaryEntry("Chuan Shu Zijiu Zhinan", "Scumbag System", listOf("The Scum Villain's Self-Saving System"), true),
        TitleDictionaryEntry("Tian Bao Fuyao Lu", "Legend of Exorcism", listOf("Tianbao Fuyao Lu"), true),
        TitleDictionaryEntry("Dinghai Fusheng Lu", "Dinghai Fusheng Records", listOf("Epic of Divinity Light"), true),
        TitleDictionaryEntry("Dinghai Qiao Shen Lu", "Dinghai Fusheng Records", listOf("Epic of Divinity Light"), true),
        TitleDictionaryEntry("Liang Bu Yi", "No Doubt in Us", listOf("No Doubt in Us"), true),
        TitleDictionaryEntry("You Shou Yan", "Fabulous Beasts", listOf("Fabulous Beasts"), true),
        TitleDictionaryEntry("Fei Ren Zai", "Non-Human", listOf("Fabulous Beasts", "Fei Ren Zai"), true),
        TitleDictionaryEntry("Luo Xiaohei Zhan Ji", "The Legend of Hei", listOf("The Legend of Luo Xiaohei"), true),
        TitleDictionaryEntry("Bai She: Yuan Qi", "White Snake", listOf("White Snake Origin"), true),
        TitleDictionaryEntry("Qing She: Jie Qi", "Green Snake", listOf("White Snake 2: Green Snake"), true),
        TitleDictionaryEntry("Da Yu Hai Tang", "Big Fish & Begonia", listOf("Big Fish and Begonia"), true),
        TitleDictionaryEntry("Nezha: Mo Tong Jiang Shi", "Ne Zha", listOf("Nezha: Birth of the Demon Child"), true),
        TitleDictionaryEntry("Jiang Ziya", "Jiang Ziya: Legend of Deification", listOf("Legend of Deification"), true),
        TitleDictionaryEntry("Hu Yao Xiao Hongniang", "Fox Spirit Matchmaker", listOf("Fox Spirit Matchmaker"), true),
        TitleDictionaryEntry("Bing Huo Mo Chu", "The Magic Chef of Ice and Fire", listOf("Magic Chef of Ice and Fire"), true),
        TitleDictionaryEntry("Xue Ying Ling Zhu", "Snow Eagle Lord", listOf("Lord Xue Ying"), true),
        TitleDictionaryEntry("Yong Sheng", "Immortality", listOf("Eternal Life", "Yong Sheng"), true),
        TitleDictionaryEntry("Xing Wu Shen Jue", "Star Martial God Technique", listOf("Star Martial God"), true),
        TitleDictionaryEntry("Wan Gu Jian Shen", "Eternal Sword God", listOf("Ten Thousand Ancient Sword God"), true),
        TitleDictionaryEntry("Wan Gu Shen Hua", "My Heroic Husband", listOf("Eternal Myth"), true),
        TitleDictionaryEntry("Jian Yu Feng Yun", "Sword Domain", listOf("The Wind and Cloud of Sword Domain"), true),
        TitleDictionaryEntry("Shen Lan Qi Yu Wushuang Zhu", "The Land of Miracles", listOf("Land of Miracles"), true),
        TitleDictionaryEntry("Chang Sheng Jie", "World of Immortals", listOf("Immortal World"), true),
        TitleDictionaryEntry("Yuan Long", "Carp Reborn", listOf("First Dragon"), true),
        TitleDictionaryEntry("Wu Lian Dian Feng", "Martial Peak", listOf("Peak of Martial Arts"), true),
        TitleDictionaryEntry("Zhen Dao Ge", "Song of the Real Blade", listOf("True Blade Song"), true),
        TitleDictionaryEntry("Hu He Yao Shi Lu", "Tiger Crane", listOf("Record of Tiger and Crane"), true),
        TitleDictionaryEntry("Shi Fang Wu Sheng", "Ten Directions Martial Saint", listOf("Martial Saint of Ten Directions"), true),
        TitleDictionaryEntry("Zhi Zun Shen Ming", "Supreme God Emperor", listOf("Supreme God"), true),
        TitleDictionaryEntry("Tian Huang Zhan Shen", "Desolate God of War", listOf("God of War"), true),
        TitleDictionaryEntry("Zui Qiang Fan Pai", "Strongest Anti M.E.T.A", listOf("Ultimate Scheming System"), true),
        TitleDictionaryEntry("Zui Hou De Zha", "The Last Summoner", listOf("Last Summoner"), true),
        TitleDictionaryEntry("Zui Hou De Zhao Huan Shi", "The Last Summoner", listOf("Last Summoner"), true),
        TitleDictionaryEntry("Tong Ling Fei", "Psychic Princess", listOf("Psychic Princess"), true),
        TitleDictionaryEntry("Xiao Lu He Xiao Lan", "Beryl and Sapphire", listOf("Beryl and Sapphire"), true),
        TitleDictionaryEntry("Guan Hai Ce", "Watching the Sea", listOf("Strategies of Watching the Sea"), true),
        TitleDictionaryEntry("Feng Shen Bang", "New Gods: Yang Jian", listOf("Investiture of the Gods"), true),
        TitleDictionaryEntry("Tai Yi Jian Xian Chuan", "Legend of Taiyi Sword Immortal", listOf("Taiyi Sword Immortal"), true),
        TitleDictionaryEntry("Wu Ying Sanqian Dao", "Three Thousand Paths of Martial Arts", listOf("Three Thousand Martial Paths"), true),
        TitleDictionaryEntry("Jiu Tian Xuan Di Jue", "Nine Heavens Profound Emperor", listOf("Nine Star Hegemon"), true),
        TitleDictionaryEntry("Zhen Wu Dian Feng", "True Martial World", listOf("Peak of True Martial Arts"), true),
        TitleDictionaryEntry("Wan Gu Zui Qiang Zong", "The All-Devouring Sect", listOf("Strongest Sect of All Times"), true),
        TitleDictionaryEntry("Da Dao Chao Tian", "The Path Toward Heaven", listOf("Path Toward Heaven"), true),
        TitleDictionaryEntry("Shen Lan Zhi Yue", "Deep Blue Moon", listOf("Deep Blue Moon"), true),
        TitleDictionaryEntry("Xinghai Qihang", "Starry Journey", listOf("Starry Journey"), true),
        TitleDictionaryEntry("Bi Lan Zhi Yu", "Azure Legacy", listOf("Azure Legacy"), true),
        TitleDictionaryEntry("Jiyi Guanli Ju", "False Memory", listOf("Memory Management Bureau"), true),
        TitleDictionaryEntry("Bai Ri Cheng Wang", "Crowned in a Hundred Days", listOf("Crowned in a Hundred Days"), true),
        TitleDictionaryEntry("Shi Yi Ci Yuan", "Eleventh Dimension", listOf("Eleventh Dimension"), true),
        TitleDictionaryEntry("Shen Qi Ta Bang", "God's Tower Ranking", listOf("God's Tower Ranking"), true),
        TitleDictionaryEntry("Lu Shidai", "Lu's Time", listOf("Lu's Time"), true),
        TitleDictionaryEntry("Kuiba", "Kuiba", listOf("Kuiba"), true),
        TitleDictionaryEntry("Wu Dong", "Martial Universe", listOf("Martial Universe"), true),
        TitleDictionaryEntry("Xian Zun Nai Ba Dang Dao", "Immortal Sovereign Dad", listOf("Immortal Dad"), true),
        TitleDictionaryEntry("Wo De Shi Xiong Tai Wen Jian Le", "My Senior Brother Is Too Steady", listOf("Big Brother"), true),
        TitleDictionaryEntry("Shen Yin", "Throne of Seal", listOf("Sealed Divine Throne"), true),
        TitleDictionaryEntry("Tun Tian Ji", "Heaven Swallowing Record", listOf("Swallowing the Heavens"), true),
        TitleDictionaryEntry("Chao Shen Xue Yuan", "Transcend the Gods", listOf("Super God Academy"), true),
        TitleDictionaryEntry("Xiong Bing Lian", "The Black Troop", listOf("Transcend the Gods: The Black Troop"), true),
        TitleDictionaryEntry("Shi Xiong", "Big Brother", listOf("My Senior Brother Is Too Steady"), true),
        TitleDictionaryEntry("Bai Yao Pu", "Fairies Albums", listOf("Manual of Hundred Demons"), true),
        TitleDictionaryEntry("Da Li Si Ri Zhi", "White Cat Legend", listOf("Journal of Dali Temple"), true),
        TitleDictionaryEntry("Wu Shuang Shu", "Peerless Martial Spirit", listOf("Peerless Art"), true),
        TitleDictionaryEntry("Tian Ying", "Sky Shadow", listOf("Heavenly Shadow"), true),
        TitleDictionaryEntry("Huan Chong Shi", "The Monster Pet Evolution", listOf("Monster Pet Evolution"), true),
        TitleDictionaryEntry("Gu Long Feng Yun Lu", "Chronicles of Gu Long", listOf("Gu Long"), true),
        TitleDictionaryEntry("Shi Jiu Tian", "19 Days", listOf("19 Days"), true),

        // =====================================================================
        // MAJOR JAPANESE ANIME (Romaji -> Official English)
        // =====================================================================
        TitleDictionaryEntry("Tensei Shitara Slime Datta Ken", "That Time I Got Reincarnated as a Slime", listOf("Tensura", "Slime Isekai")),
        TitleDictionaryEntry("Mairimashita! Iruma-kun", "Welcome to Demon School! Iruma-kun", listOf("Iruma-kun")),
        TitleDictionaryEntry("Re:Zero kara Hajimeru Isekai Seikatsu", "Re:ZERO -Starting Life in Another World-", listOf("Re:Zero", "Starting Life in Another World")),
        TitleDictionaryEntry("Mushoku Tensei: Isekai Ittara Honki Dasu", "Mushoku Tensei: Jobless Reincarnation", listOf("Jobless Reincarnation")),
        TitleDictionaryEntry("Kimetsu no Yaiba", "Demon Slayer: Kimetsu no Yaiba", listOf("Demon Slayer")),
        TitleDictionaryEntry("Shingeki no Kyojin", "Attack on Titan", listOf("AOT")),
        TitleDictionaryEntry("Boku no Hero Academia", "My Hero Academia", listOf("MHA")),
        TitleDictionaryEntry("Sousou no Frieren", "Frieren: Beyond Journey's End", listOf("Frieren")),
        TitleDictionaryEntry("Kusuriya no Hitorigoto", "The Apothecary Diaries", listOf("Apothecary Diaries")),
        TitleDictionaryEntry("Kage no Jitsuryokusha ni Naritakute!", "The Eminence in Shadow", listOf("Eminence in Shadow")),
        TitleDictionaryEntry("Tate no Yuusha no Nariagari", "The Rising of the Shield Hero", listOf("Shield Hero")),
        TitleDictionaryEntry("Kono Subarashii Sekai ni Shukufuku o!", "KONOSUBA -God's blessing on this wonderful world!", listOf("Konosuba")),
        TitleDictionaryEntry("Kaguya-sama wa Kokurasetai", "Kaguya-sama: Love is War", listOf("Love is War")),
        TitleDictionaryEntry("Youjo Senki", "Saga of Tanya the Evil", listOf("Tanya the Evil")),
        TitleDictionaryEntry("BLEACH: Sennen Kessen-hen", "BLEACH: Thousand-Year Blood War", listOf("Bleach TYBW")),
        TitleDictionaryEntry("Ore dake Level Up na Ken", "Solo Leveling", listOf("Solo Leveling")),
        TitleDictionaryEntry("Meitantei Conan", "Detective Conan", listOf("Case Closed")),
        TitleDictionaryEntry("Kaijuu 8-gou", "Kaiju No. 8", listOf("Kaiju 8")),
        TitleDictionaryEntry("Dungeon Meshi", "Delicious in Dungeon", listOf("Delicious in Dungeon")),
        TitleDictionaryEntry("Boku no Kokoro no Yabai Yatsu", "The Dangers in My Heart", listOf("Dangers in My Heart")),
        TitleDictionaryEntry("Yofukashi no Uta", "Call of the Night", listOf("Call of the Night")),
        TitleDictionaryEntry("Youkoso Jitsuryoku Shijou Shugi no Kyoushitsu e", "Classroom of the Elite", listOf("COTE")),
        TitleDictionaryEntry("Kami no Tou", "Tower of God", listOf("Tower of God")),
        TitleDictionaryEntry("Hagane no Renkinjutsushi", "Fullmetal Alchemist", listOf("FMA", "Fullmetal Alchemist: Brotherhood")),
        TitleDictionaryEntry("Code Geass: Hangyaku no Lelouch", "Code Geass: Lelouch of the Rebellion", listOf("Code Geass")),
        TitleDictionaryEntry("Shokugeki no Souma", "Food Wars! Shokugeki no Soma", listOf("Food Wars")),
        TitleDictionaryEntry("Enen no Shouboutai", "Fire Force", listOf("Fire Force")),
        TitleDictionaryEntry("Nanatsu no Taizai", "The Seven Deadly Sins", listOf("Seven Deadly Sins")),
        TitleDictionaryEntry("Yakusoku no Neverland", "The Promised Neverland", listOf("Promised Neverland")),
        TitleDictionaryEntry("Kiseijuu: Sei no Kakuritsu", "Parasyte -the maxim-", listOf("Parasyte")),
        TitleDictionaryEntry("Ansatsu Kyoushitsu", "Assassination Classroom", listOf("Assassination Classroom")),
        TitleDictionaryEntry("Seishun Buta Yarou wa Bunny Girl Senpai no Yume wo Minai", "Rascal Does Not Dream of Bunny Girl Senpai", listOf("Bunny Girl Senpai")),
        TitleDictionaryEntry("Dungeon ni Deai wo Motomeru no wa Machigatteiru Darou ka", "Is It Wrong to Try to Pick Up Girls in a Dungeon?", listOf("DanMachi")),
        TitleDictionaryEntry("Shigatsu wa Kimi no Uso", "Your Lie in April", listOf("Your Lie in April")),
        TitleDictionaryEntry("Koe no Katachi", "A Silent Voice", listOf("Silent Voice")),
        TitleDictionaryEntry("Kimi no Na wa.", "Your Name.", listOf("Your Name")),
        TitleDictionaryEntry("Tenki no Ko", "Weathering With You", listOf("Weathering With You")),
        TitleDictionaryEntry("Sen to Chihiro no Kamikakushi", "Spirited Away", listOf("Spirited Away")),
        TitleDictionaryEntry("Mononoke Hime", "Princess Mononoke", listOf("Princess Mononoke")),
        TitleDictionaryEntry("Howl no Ugoku Shiro", "Howl's Moving Castle", listOf("Howl's Moving Castle")),
        TitleDictionaryEntry("Katainaka no Ossan, Kensei ni Naru", "From Old Country Bumpkin to Master Swordsman", listOf("Old Country Bumpkin")),
        TitleDictionaryEntry("Super no Ura de Yani Suu Futari", "Smoking Behind the Supermarket with You", listOf("Smoking Behind the Supermarket")),
        TitleDictionaryEntry("Yomi no Tsugai", "Daemons of the Shadow Realm", listOf("Daemons of the Shadow Realm")),
        TitleDictionaryEntry("Oni no Hanayome", "The Demon's Bride", listOf("Demon's Bride")),
        TitleDictionaryEntry("Futsutsuka na Akujo dewa Gozaimasu ga", "Though I Am an Inept Villainess", listOf("Inept Villainess"))
    )

    // Precomputed normalized lookup tables (sorted longest key first for clean prefix matching)
    private val normalizedMap: Map<String, TitleDictionaryEntry> by lazy {
        val map = LinkedHashMap<String, TitleDictionaryEntry>()
        for (entry in entries) {
            val key = normalizeKey(entry.pinyinOrRomaji)
            if (key.isNotEmpty() && !map.containsKey(key)) {
                map[key] = entry
            }
            val compactKey = key.replace(" ", "")
            if (compactKey.length >= 6 && !map.containsKey(compactKey)) {
                map[compactKey] = entry
            }
        }
        map
    }

    private val sortedPrefixEntries: List<Pair<String, TitleDictionaryEntry>> by lazy {
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
        "piece", "punch", "notebook", "note", "detective", "classroom", "silent", "voice", "name", "weathering",
        "spirited", "away", "moving", "castle", "promised", "neverland", "deadly", "sins", "clover", "stone", "force"
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
        "les", "des", "une", "dans", "pour", "avec", "cua", "nguoi", "dau", "pha", "the", "gioi", "tinh", "tien"
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

    private fun compactAlphaNum(raw: String?): String {
        return normalizeKey(raw).replace(" ", "")
    }

    private fun hasNonLatinScript(text: String): Boolean {
        return text.any { ch ->
            val code = ch.code
            // CJK, Hiragana, Katakana, Hangul, Cyrillic, Arabic, Thai, Hebrew, Devanagari
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

    private fun hasVietnameseOrNonEnglishDiacritics(text: String): Boolean {
        return text.any { ch ->
            val code = ch.code
            (code in 0x1E00..0x1EFF) || // Vietnamese Latin Extended Additional
                ch in setOf('đ', 'Đ', 'ă', 'Ă', 'ơ', 'Ơ', 'ư', 'Ư', 'ñ', 'Ñ', 'ß', 'ç', 'Ç', 'ł', 'Ł', 'ż', 'Ż', 'ą', 'Ą', 'ę', 'Ę')
        }
    }

    /**
     * Detects whether a Latin-script string looks like Chinese Pinyin (especially when comparing against romaji).
     */
    fun isLikelyPinyin(candidate: String, romaji: String? = null): Boolean {
        val trimmed = candidate.trim()
        if (trimmed.isEmpty()) return false
        if (hasNonLatinScript(trimmed)) return false

        val normCand = normalizeKey(trimmed)
        val compactCand = normCand.replace(" ", "")
        val compactRom = compactAlphaNum(romaji)

        // Direct match or spacing/diacritic variant of romaji
        if (compactRom.length >= 4 && (compactCand == compactRom || compactCand.startsWith(compactRom) && compactCand.length - compactRom.length <= 4)) {
            // Only treat as Pinyin if it doesn't contain obvious English words
            val tokens = normCand.split(" ").filter { it.isNotEmpty() }
            val englishHits = tokens.count { it in ENGLISH_CORE_WORDS && it.length >= 3 }
            if (englishHits == 0) return true
        }

        // Check curated Donghua Pinyin keys
        if (normalizedMap[normCand]?.isDonghua == true || normalizedMap[compactCand]?.isDonghua == true) {
            return true
        }

        val tokens = normCand.split(" ").filter { it.isNotEmpty() && !it.all(Char::isDigit) }
        if (tokens.isEmpty()) return false

        val englishHits = tokens.count { it in ENGLISH_CORE_WORDS }
        if (englishHits >= 2) return false
        if (englishHits == 1 && tokens.size <= 2) return false

        // Count pinyin-like tokens
        var pinyinTokenScore = 0
        for (tok in tokens) {
            if (tok in PINYIN_MARKERS) {
                pinyinTokenScore += 2
            } else if (Regex("^(zh|ch|sh|x|q|z|c)[aeiou][a-z]{0,4}$").matches(tok)) {
                pinyinTokenScore += 1
            } else if (Regex("^[a-z]*(ang|eng|ing|ong|uan|ian|iao|iang|iong|ue|uo|ou|ai|ei)$").matches(tok) && tok !in ENGLISH_CORE_WORDS) {
                pinyinTokenScore += 1
            }
        }
        return pinyinTokenScore >= maxOf(2, tokens.size)
    }

    /**
     * Scores an AniList synonym for how strongly it represents a clean English / alternate English title.
     * Returns <= 0 if the candidate should be rejected (non-Latin, Pinyin, acronym, non-English).
     */
    fun scoreEnglishSynonym(candidate: String, romaji: String?, isDonghua: Boolean): Int {
        val clean = candidate.trim()
        if (clean.length < 3 || clean.length > 120) return -100
        if (hasNonLatinScript(clean)) return -100
        if (hasVietnameseOrNonEnglishDiacritics(clean)) return -80

        val norm = normalizeKey(clean)
        if (norm.isEmpty()) return -100
        val compact = norm.replace(" ", "")
        val compactRom = compactAlphaNum(romaji)

        // Reject pure acronyms (e.g. "BTTH", "RMJI", "PW", "SL", "LOTM")
        val lettersOnly = clean.filter { it.isLetter() }
        if (lettersOnly.isEmpty()) return -100
        if (lettersOnly.length <= 5 && lettersOnly.all { it.isUpperCase() } && !clean.contains(" ")) {
            return -60
        }

        // Reject exact/spacing variant of romaji/pinyin
        if (compactRom.length >= 4 && compact == compactRom) {
            return -50
        }

        if (isDonghua && isLikelyPinyin(clean, romaji)) {
            return -70
        }

        val tokens = norm.split(" ").filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return -100

        // Reject Japanese romaji grammar
        val jpHits = tokens.count { it in JAPANESE_GRAMMAR_TOKENS }
        if (jpHits >= 2 || (jpHits >= 1 && tokens.count { it in ENGLISH_CORE_WORDS } == 0)) {
            return -50
        }

        // Penalize obvious Spanish/French/German/Vietnamese tokens when English words are absent
        val foreignHits = tokens.count { it in NON_ENGLISH_LATIN_MARKERS && it != "the" }
        val englishWordHits = tokens.count { it in ENGLISH_CORE_WORDS }
        if (foreignHits >= 1 && englishWordHits == 0) {
            return -45
        }

        // Check if this synonym itself maps to a known English title
        if (entries.any { it.englishTitle.equals(clean, ignoreCase = true) || it.alternateEnglish.any { alt -> alt.equals(clean, ignoreCase = true) } }) {
            return 200
        }

        var score = 0
        if (englishWordHits > 0) {
            score += 40 + (englishWordHits * 18)
        } else if (isDonghua) {
            // Even single-word or uncommon English titles (e.g. "Apotheosis", "Immortality", "Rakshasa", "Kuiba")
            // if clean ASCII Title Case and NOT Pinyin, still allow as valid English candidate!
            val looksAsciiTitle = clean.all { it.code < 128 }
            if (looksAsciiTitle && !isLikelyPinyin(clean, romaji) && tokens.size in 1..10) {
                score += 18
            } else {
                return -20
            }
        } else {
            return -10
        }

        // Prefer natural title length & multi-word readable English titles
        if (tokens.size in 2..9) score += 12
        if (clean.length in 6..75) score += 8
        // Slight penalty for parenthetical fan-tag clutter like "(Donghua)" or "(TV)"
        if (clean.contains("(") && clean.contains(")")) score -= 6
        // Prefer pure ASCII English titles over accented ones
        if (clean.all { it.code < 128 }) score += 6

        return score
    }

    private fun extractTrailingSuffix(raw: String, matchedPrefixNorm: String): String {
        val clean = raw.trim()
        val normFull = normalizeKey(clean)
        if (!normFull.startsWith(matchedPrefixNorm) || normFull.length == matchedPrefixNorm.length) {
            return ""
        }
        val remainder = normFull.removePrefix(matchedPrefixNorm).trim()
        if (remainder.isEmpty()) return ""

        // Recognize clean season / part / movie / special / year suffixes
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
        Regex("^part\\s*(\\d+)$").matchEntire(remainder)?.let { m ->
            return "Part ${m.groupValues[1]}"
        }
        return ""
    }

    private fun findInTitleMap(query: String): String? {
        val clean = query.trim()
        if (clean.isEmpty()) return null
        val norm = normalizeKey(clean)
        val compact = norm.replace(" ", "")

        // 1. Exact normalized or compact match
        normalizedMap[norm]?.let { return it.englishTitle }
        normalizedMap[compact]?.let { return it.englishTitle }

        // 2. Longest prefix match (word-boundary safe) preserving season/part suffixes
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

    /**
     * Resolves display title for UI (works on already-stored Room/Supabase entities too).
     * For Donghua, automatically replaces Pinyin with English / alternate English when available.
     */
    fun resolveTitle(
        titleEnglish: String?,
        titleRomaji: String?,
        titleNative: String?,
        titlePinyin: String?,
        isDonghua: Boolean,
        preference: String = "ENGLISH",
        synonyms: List<String>? = null
    ): String {
        val prefClean = preference.uppercase().trim()
        return when (prefClean) {
            "ORIGINAL", "NATIVE" -> {
                titleNative?.trim()?.takeIf { it.isNotEmpty() }
                    ?: getEnglishOrRomajiFallback(titleEnglish, titleRomaji, titlePinyin, isDonghua, synonyms)
            }
            "ROMAJI_PINYIN", "ROMAJI", "PINYIN" -> {
                if (isDonghua) {
                    titlePinyin?.trim()?.takeIf { it.isNotEmpty() }
                        ?: titleRomaji?.trim()?.takeIf { it.isNotEmpty() }
                        ?: getEnglishOrRomajiFallback(titleEnglish, titleRomaji, titlePinyin, isDonghua, synonyms)
                } else {
                    titleRomaji?.trim()?.takeIf { it.isNotEmpty() }
                        ?: titleEnglish?.trim()?.takeIf { it.isNotEmpty() }
                        ?: getEnglishOrRomajiFallback(titleEnglish, titleRomaji, titlePinyin, isDonghua, synonyms)
                }
            }
            else -> {
                val resolved = resolve(
                    titleEnglish = titleEnglish,
                    titleRomaji = titleRomaji ?: titlePinyin,
                    synonyms = synonyms,
                    isDonghua = isDonghua
                )
                if (!resolved.isNullOrBlank()) return resolved

                val eng = titleEnglish?.trim()?.takeIf { it.isNotEmpty() }
                if (eng != null) return eng

                val rom = titleRomaji?.trim()?.takeIf { it.isNotEmpty() }
                    ?: titlePinyin?.trim()?.takeIf { it.isNotEmpty() }
                if (rom != null) return rom

                val nat = titleNative?.trim()?.takeIf { it.isNotEmpty() }
                if (nat != null) return nat

                "Untitled Anime"
            }
        }
    }

    private fun getEnglishOrRomajiFallback(
        titleEnglish: String?,
        titleRomaji: String?,
        titlePinyin: String?,
        isDonghua: Boolean,
        synonyms: List<String>?
    ): String {
        return resolve(titleEnglish, titleRomaji ?: titlePinyin, synonyms, isDonghua)
            ?: titleEnglish?.trim()?.takeIf { it.isNotEmpty() }
            ?: titleRomaji?.trim()?.takeIf { it.isNotEmpty() }
            ?: titlePinyin?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Untitled Anime"
    }

    /**
     * Resolves the best English or alternate English title from AniList fields, curated dictionary, and synonyms.
     * For Donghua, never settles for Pinyin if a real English / alternate English title is available.
     */
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

        // 1. Official English title — accept directly unless it is actually Pinyin for a Donghua
        if (eng != null) {
            val mappedFromEng = findInTitleMap(eng)
            if (mappedFromEng != null) return mappedFromEng

            val engIsPinyin = effectiveDonghua && isLikelyPinyin(eng, rom)
            if (!engIsPinyin && !hasNonLatinScript(eng)) {
                return applyPatterns(eng)
            }
        }

        // 2. Curated dictionary match on Romaji / Pinyin
        if (rom != null) {
            val mappedFromRom = findInTitleMap(rom)
            if (mappedFromRom != null) return mappedFromRom
        }

        // 3. Curated dictionary match on any synonym
        synonyms?.forEach { syn ->
            val cleanSyn = syn.trim()
            if (cleanSyn.isNotEmpty()) {
                val mapped = findInTitleMap(cleanSyn)
                if (mapped != null) return mapped
            }
        }

        // 4. Best-scoring alternate English title from AniList synonyms
        val bestSyn = pickBestEnglishSynonym(synonyms, rom, effectiveDonghua)
        if (bestSyn != null) {
            return applyPatterns(bestSyn)
        }

        // 5. Pattern replacements on Romaji if it actually translated something
        if (rom != null) {
            val trans = applyPatterns(rom)
            if (trans != rom) return trans
        }

        // 6. If official English was present (even if flagged as pinyin), fall back to it
        if (eng != null) return applyPatterns(eng)

        return null
    }

    fun pickBestEnglishSynonym(
        synonyms: List<String>?,
        romaji: String?,
        isDonghua: Boolean
    ): String? {
        if (synonyms.isNullOrEmpty()) return null
        var bestCandidate: String? = null
        var bestScore = 0
        for (syn in synonyms) {
            val clean = syn.trim()
            if (clean.isEmpty()) continue
            val sc = scoreEnglishSynonym(clean, romaji, isDonghua)
            if (sc > bestScore) {
                bestScore = sc
                bestCandidate = clean
            }
        }
        return bestCandidate
    }

    private val SEARCH_STOP_WORDS = setOf(
        "of", "the", "a", "an", "in", "to", "and", "or", "for", "on", "at", "by", "as", "is", "it", "my"
    )

    /**
     * Expands a user's English keyword query into matching Donghua/Anime Pinyin/Romaji titles
     * so AniList finds titles even when it only indexed their Pinyin name.
     */
    fun expandSearchQueries(rawQuery: String, donghuaOnly: Boolean = false, maxExtra: Int = 2): List<String> {
        val normQuery = normalizeKey(rawQuery)
        if (normQuery.length < 2) return emptyList()
        val rawTokens = normQuery.split(" ").filter { it.length >= 2 }
        if (rawTokens.isEmpty()) return emptyList()
        val contentTokens = rawTokens.filter { it !in SEARCH_STOP_WORDS }
        val effectiveTokens = if (contentTokens.isNotEmpty()) contentTokens else rawTokens

        val scored = ArrayList<Pair<String, Int>>()
        val seenPinyin = HashSet<String>()

        for (entry in entries) {
            if (donghuaOnly && !entry.isDonghua) continue
            val pinyinNorm = normalizeKey(entry.pinyinOrRomaji)
            if (pinyinNorm == normQuery || !seenPinyin.add(pinyinNorm)) continue

            val candidates = listOf(entry.englishTitle) + entry.alternateEnglish
            var bestScore = 0
            for (cand in candidates) {
                val candNorm = normalizeKey(cand)
                if (contentTokens.isNotEmpty() && contentTokens.none { tok -> candNorm.contains(tok) }) {
                    continue
                }
                val sc = scoreSingleTextMatch(normQuery, effectiveTokens, cand)
                if (sc > bestScore) bestScore = sc
            }
            if (bestScore >= 40) {
                // Boost Donghua slightly since AniList English indexing is weakest for Donghua
                scored.add(entry.pinyinOrRomaji to (bestScore + if (entry.isDonghua) 15 else 0))
            }
        }

        return scored
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctBy { normalizeKey(it) }
            .take(maxExtra)
    }

    /**
     * Scores how well a catalog item matches the user's typed keywords across English, Romaji/Pinyin, and synonyms.
     */
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
        for ((text, weightBonus) in fields) {
            val base = scoreSingleTextMatch(normQuery, queryTokens, text)
            if (base > 0) {
                val total = base + weightBonus
                if (total > best) best = total
            }
        }
        return best
    }

    /**
     * Returns true if every keyword typed by the user appears in any of the item's titles/synonyms/genres.
     */
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

        var exactWordMatches = 0
        var prefixWordMatches = 0
        var substringMatches = 0

        for (qt in queryTokens) {
            when {
                textTokens.any { it == qt } -> exactWordMatches++
                textTokens.any { it.startsWith(qt) } -> prefixWordMatches++
                normText.contains(qt) -> substringMatches++
            }
        }

        val totalMatched = exactWordMatches + prefixWordMatches + substringMatches
        if (totalMatched == 0) return 0

        val allMatched = totalMatched == queryTokens.size
        return when {
            allMatched && exactWordMatches == queryTokens.size -> 95
            allMatched && (exactWordMatches + prefixWordMatches) == queryTokens.size -> 82
            allMatched -> 70
            else -> (exactWordMatches * 28) + (prefixWordMatches * 20) + (substringMatches * 10)
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
