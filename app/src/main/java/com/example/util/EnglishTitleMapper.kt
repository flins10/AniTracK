package com.example.util

object EnglishTitleMapper {
    private val titleMap = mapOf(
        // Slime series
        "Tensei Shitara Slime Datta Ken 4th Season" to "That Time I Got Reincarnated as a Slime Season 4",
        "Tensei Shitara Slime Datta Ken 3rd Season" to "That Time I Got Reincarnated as a Slime Season 3",
        "Tensei Shitara Slime Datta Ken 2nd Season" to "That Time I Got Reincarnated as a Slime Season 2",
        "Tensei Shitara Slime Datta Ken" to "That Time I Got Reincarnated as a Slime",
        "Tensei Shitara Slime Datta Ken: Tensura Nikki" to "The Slime Diaries: That Time I Got Reincarnated as a Slime",
        "Tensei Shitara Slime Datta Ken: Coleus no Yume" to "That Time I Got Reincarnated as a Slime: Visions of Coleus",

        // Iruma-kun series
        "Mairimashita! Iruma-kun 4" to "Welcome to Demon School! Iruma-kun Season 4",
        "Mairimashita! Iruma-kun 3" to "Welcome to Demon School! Iruma-kun Season 3",
        "Mairimashita! Iruma-kun 2" to "Welcome to Demon School! Iruma-kun Season 2",
        "Mairimashita! Iruma-kun" to "Welcome to Demon School! Iruma-kun",

        // Re:Zero
        "Re:Zero kara Hajimeru Isekai Seikatsu 4th Season" to "Re:ZERO -Starting Life in Another World- Season 4",
        "Re:Zero kara Hajimeru Isekai Seikatsu 3rd Season" to "Re:ZERO -Starting Life in Another World- Season 3",
        "Re:Zero kara Hajimeru Isekai Seikatsu 2nd Season" to "Re:ZERO -Starting Life in Another World- Season 2",
        "Re:Zero kara Hajimeru Isekai Seikatsu" to "Re:ZERO -Starting Life in Another World-",

        // Mushoku Tensei
        "Mushoku Tensei III: Isekai Ittara Honki Dasu" to "Mushoku Tensei: Jobless Reincarnation Season 3",
        "Mushoku Tensei II: Isekai Ittara Honki Dasu" to "Mushoku Tensei: Jobless Reincarnation Season 2",
        "Mushoku Tensei: Isekai Ittara Honki Dasu" to "Mushoku Tensei: Jobless Reincarnation",

        // Major Japanese Anime (Old & New)
        "Mahou Shoujo Lyrical Nanoha EX: Detonation" to "Magical Girl Lyrical Nanoha EX",
        "Mahou Shoujo Lyrical Nanoha EX" to "Magical Girl Lyrical Nanoha EX",
        "Futsutsuka na Akujo dewa Gozaimasu ga" to "Though I Am an Inept Villainess",
        "Futsutsuka na Akujo dewa Gozai" to "Though I Am an Inept Villainess",
        "Oni no Hanayome" to "The Demon's Bride",
        "Meitantei Conan" to "Detective Conan",
        "Kimetsu no Yaiba: Yuukaku-hen" to "Demon Slayer: Entertainment District Arc",
        "Kimetsu no Yaiba: Katanakaji no Sato-hen" to "Demon Slayer: Swordsmith Village Arc",
        "Kimetsu no Yaiba: Hashira Geiko-hen" to "Demon Slayer: Hashira Training Arc",
        "Kimetsu no Yaiba" to "Demon Slayer: Kimetsu no Yaiba",
        "Shingeki no Kyojin" to "Attack on Titan",
        "Boku no Hero Academia" to "My Hero Academia",
        "Jujutsu Kaisen 2nd Season" to "Jujutsu Kaisen Season 2",
        "Jujutsu Kaisen" to "Jujutsu Kaisen",
        "Sousou no Frieren" to "Frieren: Beyond Journey's End",
        "Kusuriya no Hitorigoto" to "The Apothecary Diaries",
        "Kage no Jitsuryokusha ni Naritakute!" to "The Eminence in Shadow",
        "Tate no Yuusha no Nariagari" to "The Rising of the Shield Hero",
        "Kono Subarashii Sekai ni Shukufuku o!" to "KONOSUBA -God's blessing on this wonderful world!",
        "Kaguya-sama wa Kokurasetai: Tensai-tachi no Renai Zunousen" to "Kaguya-sama: Love is War",
        "Kaguya-sama wa Kokurasetai" to "Kaguya-sama: Love is War",
        "Youjo Senki II" to "Saga of Tanya the Evil Season 2",
        "Youjo Senki" to "Saga of Tanya the Evil",
        "BLEACH: Sennen Kessen-hen - Kashin-tan" to "BLEACH: Thousand-Year Blood War - The Calamity",
        "BLEACH: Sennen Kessen-hen" to "BLEACH: Thousand-Year Blood War",
        "Bleach" to "Bleach",
        "Super no Ura de Yani Suu Futari" to "Smoking Behind the Supermarket with You",
        "Yomi no Tsugai" to "Daemons of the Shadow Realm",
        "Katainaka no Ossan, Kensei ni Naru II" to "From Old Country Bumpkin to Master Swordsman Season 2",
        "Katainaka no Ossan, Kensei ni Naru" to "From Old Country Bumpkin to Master Swordsman",
        "Gaikotsu Kishi-sama, Tadaima Isekai e Odekakechuu II" to "Skeleton Knight in Another World Season 2",
        "Otomege Sekai wa Mob ni Kibishii Sekai desu 2" to "Trapped in a Dating Sim Season 2",
        "Tsuihou Sareta Tensei Juukishi wa Game Chishiki de Musou Suru" to "The Exiled Heavy Knight Knows How to Game the System",
        "Ore dake Level Up na Ken" to "Solo Leveling",
        "Solo Leveling" to "Solo Leveling",
        "Chainsaw Man" to "Chainsaw Man",
        "SPY FAMILY" to "Spy x Family",
        "Spy x Family" to "Spy x Family",
        "Oshi no Ko" to "Oshi no Ko [My Star]",
        "Bocchi the Rock!" to "Bocchi the Rock!",
        "Dandadan" to "Dandadan",
        "Kaijuu 8-gou" to "Kaiju No. 8",
        "Wind Breaker" to "Wind Breaker",
        "Mashle: Magic and Muscles" to "Mashle: Magic and Muscles",
        "Mashle" to "Mashle: Magic and Muscles",
        "Dungeon Meshi" to "Delicious in Dungeon",
        "Shangri-La Frontier" to "Shangri-La Frontier",
        "Boku no Kokoro no Yabai Yatsu" to "The Dangers in My Heart",
        "Yofukashi no Uta" to "Call of the Night",
        "Rakudai Kishi no Cavalry" to "Chivalry of a Failed Knight",
        "Mahouka Koukou no Rettousei" to "The Irregular at Magic High School",
        "Youkoso Jitsuryoku Shijou Shugi no Kyoushitsu e" to "Classroom of the Elite",
        "Kami no Tou: Tower of God" to "Tower of God",
        "Kami no Tou" to "Tower of God",
        "Tower of God" to "Tower of God",
        "God of High School" to "The God of High School",
        "Noblesse" to "Noblesse",
        "One Piece" to "One Piece",
        "Naruto: Shippuuden" to "Naruto: Shippuden",
        "Naruto" to "Naruto",
        "Dragon Ball Daima" to "Dragon Ball Daima",
        "Dragon Ball Super" to "Dragon Ball Super",
        "Dragon Ball Z" to "Dragon Ball Z",
        "Dragon Ball" to "Dragon Ball",
        "Hunter x Hunter (2011)" to "Hunter x Hunter",
        "Hunter x Hunter" to "Hunter x Hunter",
        "Fullmetal Alchemist: Brotherhood" to "Fullmetal Alchemist: Brotherhood",
        "Hagane no Renkinjutsushi" to "Fullmetal Alchemist",
        "Steins;Gate" to "Steins;Gate",
        "Death Note" to "Death Note",
        "Code Geass: Hangyaku no Lelouch" to "Code Geass: Lelouch of the Rebellion",
        "Gintama" to "Gintama",
        "Sword Art Online" to "Sword Art Online",
        "No Game No Life" to "No Game No Life",
        "Toradora!" to "Toradora!",
        "Mob Psycho 100" to "Mob Psycho 100",
        "Vinland Saga" to "Vinland Saga",
        "One Punch Man" to "One-Punch Man",
        "Shokugeki no Souma" to "Food Wars! Shokugeki no Soma",
        "Haikyuu!!" to "Haikyu!!",
        "Kuroko no Basket" to "Kuroko's Basketball",
        "Slam Dunk" to "Slam Dunk",
        "Neon Genesis Evangelion" to "Neon Genesis Evangelion",
        "Shin Seiki Evangelion" to "Neon Genesis Evangelion",
        "Cowboy Bebop" to "Cowboy Bebop",
        "Samurai Champloo" to "Samurai Champloo",
        "Trigun" to "Trigun",
        "Berserk" to "Berserk",
        "Monster" to "Monster",
        "Ghost in the Shell" to "Ghost in the Shell",
        "Koukyoushihen Eureka Seven" to "Eureka Seven",
        "Tengen Toppa Gurren Lagann" to "Gurren Lagann",
        "Kill la Kill" to "Kill la Kill",
        "Fate/stay night" to "Fate/stay night",
        "Fate/Zero" to "Fate/Zero",
        "Overlord" to "Overlord",
        "Yakusoku no Neverland" to "The Promised Neverland",
        "Tokyo Ghoul" to "Tokyo Ghoul",
        "Noragami" to "Noragami",
        "Akame ga Kill!" to "Akame ga Kill!",
        "Parasyte -the maxim-" to "Parasyte -the maxim-",
        "Kiseijuu: Sei no Kakuritsu" to "Parasyte -the maxim-",
        "Nanatsu no Taizai" to "The Seven Deadly Sins",
        "Black Clover" to "Black Clover",
        "Dr. STONE" to "Dr. STONE",
        "Fire Force" to "Fire Force",
        "Enen no Shouboutai" to "Fire Force",
        "Sazae-san" to "Sazae-san",
        "Chibi Maruko-chan" to "Chibi Maruko-chan",
        "Doraemon" to "Doraemon",
        "Crayon Shin-chan" to "Shin-chan",

        // Chinese Donghua (Extensive English Title Mapping)
        "Wanmei Shijie" to "Perfect World",
        "Wu Shen Zhu Zai" to "Martial Master",
        "Lian Qi Shi Wan Nian" to "100,000 Years of Qi Refining",
        "Zhu Xian" to "Jade Dynasty",
        "Da Zhu Zai" to "The Great Ruler",
        "Ni Tian Xie Shen" to "Against the Gods",
        "Shiguang Dailiren III" to "Link Click Season 3",
        "Shiguang Dailiren II" to "Link Click Season 2",
        "Shiguang Dailiren" to "Link Click",
        "Tian Guan Ci Fu" to "Heaven Official's Blessing",
        "Xian Ni" to "Renegade Immortal",
        "Douluo Dalu 2: Jueshi Tangmen" to "Soul Land 2: The Peerless Tang Clan",
        "Douluo Dalu II" to "Soul Land 2",
        "Douluo Dalu" to "Soul Land",
        "Zhe Tian" to "Shrouding the Heavens",
        "Tunshi Xingkong" to "Swallowed Star",
        "Yi Nian Yong Heng" to "A Will Eternal",
        "Fan Ren Xiu Xian Chuan" to "A Record of a Mortal's Journey to Immortality",
        "Dou Po Cang Qiong" to "Battle Through the Heavens",
        "Wan Gu Shen Hua" to "My Heroic Husband",
        "Wan Jie Duzun" to "Ten Thousand Worlds",
        "Shen Yin Wang Zuo" to "Throne of Seal",
        "Yao Shen Ji" to "Tales of Demons and Gods",
        "Xian Wang de Richang Shenghuo" to "The Daily Life of the Immortal King",
        "Wan Jie Xian Zong" to "Wonderland of Ten Thousand Deities",
        "Wu Dong Qian Kun" to "Martial Universe",
        "Xing Chen Bian" to "Stellar Transformations",
        "Cang Lan Jue" to "Love Between Fairy and Devil",
        "Mo Dao Zu Shi" to "The Grandmaster of Demonic Cultivation",
        "Quan Zhi Gao Shou" to "The King's Avatar",
        "Cikang Qi" to "Scissor Seven",
        "Wu Shan Wu Xing" to "Fog Hill of Five Elements",
        "Bai Lian Cheng Shen" to "Apotheosis",
        "Cang Yuan Tu" to "The Demon Hunter",
        "Ni Tian Zhizun" to "Against the Gods",
        "Zhi Zun Shen Ming" to "Supreme God Emperor",
        "Duo Xuan Bian" to "Thousand Autumns",
        "Bi Lan Zhi Yu" to "Azure Legacy",
        "Zui Hou De Zha" to "The Last Summoner",
        "Jiyi Guanli Ju" to "False Memory",
        "Bai Ri Cheng Wang" to "Crowned in a Hundred Days",
        "Li Xiongmao" to "Raised by Demons: Panda Li",
        "Shan He Jian Xin" to "Thousand Autumns",
        "Ling Long: Incarnation" to "Ling Cage: Incarnation",
        "Huayao" to "The Legend of Hei",
        "Feng Shen Bang" to "New Gods: Yang Jian",
        "Nezha: Mo Tong Jiang Shi" to "Ne Zha",
        "Jiang Ziya" to "Legend of Deification",
        "Bai She: Yuan Qi" to "White Snake",
        "Qing She: Jie Qi" to "Green Snake",
        "Da Yu Hai Tang" to "Big Fish & Begonia",
        "Zhen Hun Jie" to "Rakshasa Street",
        "Quanzhi Fashi" to "Full-Time Magister",
        "Xinghai Qihang" to "Starry Journey",
        "Shen Lan Zhi Yue" to "Deep Blue Moon",
        "Yuan Zun" to "Dragon Prince Yuan",
        "Sheng Xu" to "The Sacred Ruins",
        "Bu Liang Ren" to "The Degenerate",
        "Zhen Dao Ge" to "Song of the Real Blade",
        "Fei Ren Zai" to "Non-Human",
        "Lu Shidai" to "Lu's Time",
        "Chuan Shu Zijiu Zhinan" to "Scumbag System",
        "Tian Bao Fuyao Lu" to "Legend of Exorcism",
        "Dinghai Qiao Shen Lu" to "Dinghai Fusheng Records",
        "Liang Bu Yi" to "No Doubt In Us",
        "You Shou Yan" to "Fabulous Beasts",
        "Xiao Lu He Xiao Lan" to "Beryl and Sapphire",
        "Tong Ling Fei" to "Psychic Princess",
        "Shi Yi Ci Yuan" to "Eleventh Dimension",
        "Cang Lan Zhi Jue" to "Love Between Fairy and Devil",
        "Shi Zong Zhi Wu" to "Dance of the Dead",
        "Guan Hai Ce" to "Watching the Sea",
        "Shen Qi Ta Bang" to "God's Tower Ranking",
        "Zui Qiang Fan Pai" to "Strongest Anti M.E.T.A",
        "Tian Huang Zhan Shen" to "Desolate God of War",
        "Shen Wu Tian Zun" to "Supreme Martial God",
        "Du Bu Xiao Yao" to "One Step Toward Freedom",
        "Wan Jie Fa Shen" to "God of All Realms",
        "Ling Jian Zun" to "Spirit Sword Sovereign",
        "Kuiba" to "Kuiba",

        // More Classic and Seasonal Japanese Anime
        "Choujigen Game Neptune" to "Hyperdimension Neptunia",
        "Ansatsu Kyoushitsu" to "Assassination Classroom",
        "Seishun Buta Yarou wa Bunny Girl Senpai no Yume wo Minai" to "Rascal Does Not Dream of Bunny Girl Senpai",
        "Hataraku Saibou" to "Cells at Work!",
        "Tensei Shitara Ken Deshita" to "Reincarnated as a Sword",
        "Kinsou no Vermeil" to "Vermeil in Gold",
        "Itai no wa Iya nano de Bougyoryoku ni Kyokufuri Shitai to Omoimasu" to "BOFURI: I Don't Want to Get Hurt, so I'll Max Out My Defense",
        "Jidouhanbaiki ni Umarekawatta Ore wa Meikyuu wo Samayou" to "Reborn as a Vending Machine, I Now Wander the Dungeon",
        "Tensei Kizoku, Kantei Skill de Nariagaru" to "As a Reincarnated Aristocrat, I'll Use My Appraisal Skill",
        "Hitoribocchi no Isekai Kouryaku" to "Loner Life in Another World",
        "Dungeon ni Deai wo Motomeru no wa Machigatteiru Darou ka" to "Is It Wrong to Try to Pick Up Girls in a Dungeon?",
        "Dungeon ni Deai" to "Is It Wrong to Try to Pick Up Girls in a Dungeon?",
        "DanMachi" to "Is It Wrong to Try to Pick Up Girls in a Dungeon?",
        "Sentouin, Hakenshimasu!" to "Combatants Will Be Dispatched!",
        "Shinmai Renkinjutsushi no Tenpo Keiei" to "Management of a Novice Alchemist",
        "Ningen Fushin no Boukensha-tachi ga Sekai wo Sukuu You desu" to "Apparently, Disillusioned Adventurers Will Save the World",
        "Isekai Ojisan" to "Uncle from Another World",
        "Akuyaku Reijou Nanode Last Boss wo Kattemimashita" to "I'm the Villainess, So I'm Taming the Final Boss",
        "Watashi no Oshi wa Akuyaku Reijou." to "I'm in Love with the Villainess",
        "Tearmoon Teikoku Monogatari" to "Tearmoon Empire",
        "Shangri-La Frontier: Kusoge Hunter, Kamige ni Idoman to Su" to "Shangri-La Frontier",
        "Akuyaku Reijou Level 99" to "Villainess Level 99: I May Be the Hidden Boss but I'm Not the Demon Lord",
        "Oroka na Tenshi wa Akuma to Odoru" to "The Foolish Angel Dances with the Devil",
        "Kuroshitsuji" to "Black Butler",
        "Mahoutsukai no Yome" to "The Ancient Magus' Bride",
        "Shigatsu wa Kimi no Uso" to "Your Lie in April",
        "Ano Hi Mita Hana no Namae wo Bokutachi wa Mada Shiranai." to "Anohana: The Flower We Saw That Day",
        "Koe no Katachi" to "A Silent Voice",
        "Kimi no Na wa." to "Your Name.",
        "Tenki no Ko" to "Weathering With You",
        "Suzume no Tojimari" to "Suzume",
        "Hotarubi no Mori e" to "Into the Forest of Fireflies' Light",
        "Sen to Chihiro no Kamikakushi" to "Spirited Away",
        "Mononoke Hime" to "Princess Mononoke",
        "Tonari no Totoro" to "My Neighbor Totoro",
        "Howl no Ugoku Shiro" to "Howl's Moving Castle",
        "Kimi ni Todoke" to "Kimi ni Todoke: From Me to You",
        "Ouran Koukou Host Club" to "Ouran High School Host Club",
        "Clannad: After Story" to "Clannad: After Story",
        "Clannad" to "Clannad",
        "Angel Beats!" to "Angel Beats!",
        "Charlotte" to "Charlotte",
        "Guilty Crown" to "Guilty Crown",
        "Mirai Nikki" to "The Future Diary",
        "Elfen Lied" to "Elfen Lied",
        "Higurashi no Naku Koro ni" to "Higurashi: When They Cry",
        "Another" to "Another",
        "Death Parade" to "Death Parade",
        "Psycho-Pass" to "Psycho-Pass",
        "Darker than Black" to "Darker than Black",
        "Hellsing Ultimate" to "Hellsing Ultimate",
        "Black Lagoon" to "Black Lagoon",
        "Baccano!" to "Baccano!",
        "Durarara!!" to "Durarara!!",
        "Samurai Champloo" to "Samurai Champloo",
        "Great Teacher Onizuka" to "Great Teacher Onizuka",
        "Rurouni Kenshin" to "Rurouni Kenshin",
        "Yu Yu Hakusho" to "Yu Yu Hakusho",
        "InuYasha" to "Inuyasha",
        "Bleach: Sennen Kessen-hen" to "BLEACH: Thousand-Year Blood War",
        "Cardcaptor Sakura" to "Cardcaptor Sakura",
        "Sailor Moon" to "Sailor Moon",
        "Bishoujo Senshi Sailor Moon" to "Sailor Moon"
    )

    private val patternReplacements = listOf(
        Regex("(?i)Tensei Shitara Slime Datta Ken") to "That Time I Got Reincarnated as a Slime",
        Regex("(?i)Mairimashita! Iruma-kun") to "Welcome to Demon School! Iruma-kun",
        Regex("(?i)Meitantei Conan") to "Detective Conan",
        Regex("(?i)Shiguang Dailiren") to "Link Click",
        Regex("(?i)Tian Guan Ci Fu") to "Heaven Official's Blessing",
        Regex("(?i)Xian Ni") to "Renegade Immortal",
        Regex("(?i)Douluo Dalu 2") to "Soul Land 2",
        Regex("(?i)Douluo Dalu") to "Soul Land",
        Regex("(?i)Zhe Tian") to "Shrouding the Heavens",
        Regex("(?i)Tunshi Xingkong") to "Swallowed Star",
        Regex("(?i)Yi Nian Yong Heng") to "A Will Eternal",
        Regex("(?i)Fan Ren Xiu Xian Chuan") to "A Record of a Mortal's Journey to Immortality",
        Regex("(?i)Dou Po Cang Qiong") to "Battle Through the Heavens",
        Regex("(?i)Shen Yin Wang Zuo") to "Throne of Seal",
        Regex("(?i)Yao Shen Ji") to "Tales of Demons and Gods",
        Regex("(?i)Xian Wang de Richang Shenghuo") to "The Daily Life of the Immortal King",
        Regex("(?i)Wu Dong Qian Kun") to "Martial Universe",
        Regex("(?i)Xing Chen Bian") to "Stellar Transformations",
        Regex("(?i)Mo Dao Zu Shi") to "The Grandmaster of Demonic Cultivation",
        Regex("(?i)Quan Zhi Gao Shou") to "The King's Avatar",
        Regex("(?i)Bai Lian Cheng Shen") to "Apotheosis",
        Regex("(?i)Cang Yuan Tu") to "The Demon Hunter",
        Regex("(?i)Wanmei Shijie") to "Perfect World",
        Regex("(?i)Wu Shen Zhu Zai") to "Martial Master",
        Regex("(?i)Zhu Xian") to "Jade Dynasty",
        Regex("(?i)Ni Tian Zhizun") to "Against the Gods",
        Regex("(?i)Ni Tian Xie Shen") to "Against the Gods",
        Regex("(?i)Kimetsu no Yaiba") to "Demon Slayer",
        Regex("(?i)Boku no Hero Academia") to "My Hero Academia",
        Regex("(?i)Shingeki no Kyojin") to "Attack on Titan",
        Regex("(?i)Sousou no Frieren") to "Frieren: Beyond Journey's End",
        Regex("(?i)Kusuriya no Hitorigoto") to "The Apothecary Diaries",
        Regex("(?i)Kage no Jitsuryokusha") to "The Eminence in Shadow",
        Regex("(?i)4th Season") to "Season 4",
        Regex("(?i)3rd Season") to "Season 3",
        Regex("(?i)2nd Season") to "Season 2",
        Regex("(?i)1st Season") to "Season 1",
        Regex("(?i)Dai (\\d+) Ki") to "Season $1"
    )

    /**
     * Resolves title with PRIMARY focus on English titles for ALL anime & donghua (Requirement B).
     */
    fun resolveTitle(
        titleEnglish: String?,
        titleRomaji: String?,
        titleNative: String?,
        titlePinyin: String?,
        isDonghua: Boolean,
        preference: String = "ENGLISH"
    ): String {
        val prefClean = preference.uppercase().trim()
        return when (prefClean) {
            "ORIGINAL", "NATIVE" -> {
                titleNative?.trim()?.takeIf { it.isNotEmpty() }
                    ?: getEnglishOrRomajiFallback(titleEnglish, titleRomaji, titlePinyin)
            }
            "ROMAJI_PINYIN", "ROMAJI", "PINYIN" -> {
                if (isDonghua) {
                    titlePinyin?.trim()?.takeIf { it.isNotEmpty() }
                        ?: titleRomaji?.trim()?.takeIf { it.isNotEmpty() }
                        ?: getEnglishOrRomajiFallback(titleEnglish, titleRomaji, titlePinyin)
                } else {
                    titleRomaji?.trim()?.takeIf { it.isNotEmpty() }
                        ?: titleEnglish?.trim()?.takeIf { it.isNotEmpty() }
                        ?: getEnglishOrRomajiFallback(titleEnglish, titleRomaji, titlePinyin)
                }
            }
            else -> {
                // Strict Title Selection Priority for ENGLISH (Requirement B):
                // 1. "title.english" when it is non-null and non-empty.
                // 2. "title.romaji" (or pinyin) only when English title is genuinely unavailable.
                // 3. "title.native" only when both English and romaji are unavailable.
                // 4. Otherwise, display a clear unavailable-title fallback.
                val eng = titleEnglish?.trim()?.takeIf { it.isNotEmpty() }
                if (eng != null) return eng

                val rom = titleRomaji?.trim()?.takeIf { it.isNotEmpty() }
                    ?: titlePinyin?.trim()?.takeIf { it.isNotEmpty() }
                if (rom != null) return rom

                val nat = titleNative?.trim()?.takeIf { it.isNotEmpty() }
                if (nat != null) return nat

                return "Untitled Anime"
            }
        }
    }

    private fun getEnglishOrRomajiFallback(
        titleEnglish: String?,
        titleRomaji: String?,
        titlePinyin: String?
    ): String {
        return titleEnglish?.trim()?.takeIf { it.isNotEmpty() }
            ?: titleRomaji?.trim()?.takeIf { it.isNotEmpty() }
            ?: titlePinyin?.trim()?.takeIf { it.isNotEmpty() }
            ?: "Untitled Anime"
    }

    /**
     * Resolves an English title from available AniList title fields, dictionary mappings, or synonyms.
     * Always preserves official AniList English title if available.
     */
    fun resolve(titleEnglish: String?, titleRomaji: String?, synonyms: List<String>? = null): String? {
        val eng = titleEnglish?.trim()?.takeIf { it.isNotEmpty() }
        val rom = titleRomaji?.trim()?.takeIf { it.isNotEmpty() }

        // 1. Direct official English title takes precedence
        if (eng != null) {
            val mapped = findInTitleMap(eng)
            if (mapped != null) return mapped
            return applyPatterns(eng)
        }

        // 2. Direct dictionary match on Romaji or Synonyms
        if (rom != null) {
            val mapped = findInTitleMap(rom)
            if (mapped != null) return mapped
        }
        synonyms?.forEach { syn ->
            val cleanSyn = syn.trim()
            if (cleanSyn.isNotEmpty()) {
                val mapped = findInTitleMap(cleanSyn)
                if (mapped != null) return mapped
            }
        }

        // 3. Pattern replacements on Romaji (e.g. "Sousou no Frieren" -> "Frieren: Beyond Journey's End", "Season 4", etc.)
        if (rom != null) {
            val trans = applyPatterns(rom)
            if (trans != rom) return trans
        }

        // 4. Check synonyms for an English synonym (Latin script, non-romaji)
        synonyms?.forEach { syn ->
            val cleanSyn = syn.trim()
            if (cleanSyn.isNotEmpty() && looksLikeEnglish(cleanSyn)) {
                return applyPatterns(cleanSyn)
            }
        }

        return null
    }

    private fun findInTitleMap(query: String): String? {
        val clean = query.trim()
        if (clean.isEmpty()) return null

        // 1. Exact match
        titleMap[clean]?.let { return it }

        // 2. Case-insensitive exact match
        titleMap.entries.firstOrNull { clean.equals(it.key, ignoreCase = true) }?.value?.let { return it }

        // 3. Substring containment match
        titleMap.entries.firstOrNull { clean.contains(it.key, ignoreCase = true) }?.value?.let { return it }

        return null
    }

    private fun looksLikeEnglish(str: String): Boolean {
        if (str.isBlank()) return false
        val englishWords = setOf(
            "the", "of", "in", "a", "an", "season", "world", "hero", "life", "king",
            "immortal", "blade", "chronicles", "legend", "war", "battle", "demon", "spirit",
            "lord", "god", "land", "click", "star", "case", "closed", "hunter", "seal",
            "quest", "level", "online", "academy", "school", "punch", "piece", "slayer",
            "titan", "girl", "boy", "man", "reincarnated", "another", "journey", "diaries",
            "shadow", "magic", "sword", "shield", "slime", "gate", "death", "note", "ball",
            "clover", "tail", "force", "brotherhood", "alchemist", "bleach", "avatar", "master",
            "perfect", "apothecary", "villainess", "bride", "supermarket", "daemons", "re:zero",
            "spy", "family", "dr.", "stone", "moon", "prince", "exorcism", "mononoke"
        )
        val lower = str.lowercase()
        return englishWords.any { lower.contains(it) }
    }

    private fun isJapaneseGrammar(str: String): Boolean {
        val lower = " " + str.lowercase() + " "
        val particles = listOf(" no ", " wa ", " ga ", " wo ", " ni ", " de ", " kara ", " made ", " to ", " datta ", " ken ", " naritakute ", " shite ")
        return particles.any { lower.contains(it) }
    }

    private fun cleanTitle(title: String): String {
        return applyPatterns(title)
    }

    private fun applyPatterns(text: String): String {
        var result = text
        for ((regex, replacement) in patternReplacements) {
            result = regex.replace(result, replacement)
        }
        return result
    }

    private fun isPureRomaji(str: String): Boolean {
        val lower = str.lowercase()
        return lower.startsWith("tensei shitara slime") ||
                lower.startsWith("mairimashita! iruma") ||
                lower.startsWith("meitantei conan") ||
                lower.startsWith("shiguang dailiren") ||
                lower.startsWith("tian guan ci fu") ||
                lower.startsWith("xian ni") ||
                lower.startsWith("douluo dalu") ||
                lower.startsWith("zhe tian") ||
                lower.startsWith("tunshi xingkong") ||
                lower.startsWith("wanmei shijie") ||
                lower.startsWith("wu shen zhu zai") ||
                lower.startsWith("zhu xian") ||
                lower.startsWith("sousou no frieren") ||
                lower.startsWith("kusuriya no hitorigoto") ||
                lower.startsWith("boku no hero") ||
                lower.startsWith("shingeki no kyojin") ||
                lower.startsWith("kimetsu no yaiba")
    }
}
