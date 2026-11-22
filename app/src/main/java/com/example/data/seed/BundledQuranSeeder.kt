package com.example.data.seed

import android.content.Context
import com.example.data.QariCatalog
import com.example.data.local.AyahEntity
import com.example.data.local.QuranDao
import com.example.data.local.SurahEntity
import org.json.JSONArray

object BundledQuranSeeder {

    suspend fun seedIfEmpty(context: Context, dao: QuranDao) {
        if (dao.getSurahCount() >= 114) return

        val metaJson = context.assets.open("quran_surahs_meta.json").bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(metaJson)
        val starterSurahNumbers = setOf(1, 112, 113, 114)
        val surahs = ArrayList<SurahEntity>(jsonArray.length())

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val nomor = obj.getInt("nomor")
            val padded = nomor.toString().padStart(3, '0')
            surahs.add(
                SurahEntity(
                    nomor = nomor,
                    namaArab = obj.getString("namaArab"),
                    namaLatin = obj.getString("namaLatin"),
                    jumlahAyat = obj.getInt("jumlahAyat"),
                    tempatTurun = obj.getString("tempatTurun"),
                    arti = obj.getString("arti"),
                    deskripsi = "",
                    audioFull01 = "https://equran.id/audiofull/01/$padded.mp3",
                    audioFull02 = "https://equran.id/audiofull/02/$padded.mp3",
                    audioFull03 = "https://equran.id/audiofull/03/$padded.mp3",
                    audioFull04 = "https://equran.id/audiofull/04/$padded.mp3",
                    audioFull05 = "https://equran.id/audiofull/05/$padded.mp3",
                    audioFull06 = "https://equran.id/audiofull/06/$padded.mp3",
                    isDownloaded = nomor in starterSurahNumbers
                )
            )
        }
        dao.insertSurahs(surahs)

        // Seed essential starter surahs so offline first-launch reading & Tajwid work immediately
        dao.insertAyahs(starterAyahs())
    }

    private fun ayah(
        surah: Int,
        surahName: String,
        nomor: Int,
        arab: String,
        latin: String,
        indo: String
    ): AyahEntity = AyahEntity(
        id = surah * 1000 + nomor,
        surahNomor = surah,
        surahNamaLatin = surahName,
        nomorAyat = nomor,
        teksArab = arab,
        teksLatin = latin,
        teksIndonesia = indo,
        audio01 = QariCatalog.buildFallbackAyahAudioUrl("01", surah, nomor),
        audio02 = QariCatalog.buildFallbackAyahAudioUrl("02", surah, nomor),
        audio03 = QariCatalog.buildFallbackAyahAudioUrl("03", surah, nomor),
        audio04 = QariCatalog.buildFallbackAyahAudioUrl("04", surah, nomor),
        audio05 = QariCatalog.buildFallbackAyahAudioUrl("05", surah, nomor),
        audio06 = QariCatalog.buildFallbackAyahAudioUrl("06", surah, nomor)
    )

    private fun starterAyahs(): List<AyahEntity> = listOf(
        // Al-Fatihah (1)
        ayah(1, "Al-Fatihah", 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "Bismillāhir-raḥmānir-raḥīm(i).", "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang."),
        ayah(1, "Al-Fatihah", 2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "Al-ḥamdu lillāhi rabbil-'ālamīn(a).", "Segala puji bagi Allah, Tuhan seluruh alam."),
        ayah(1, "Al-Fatihah", 3, "الرَّحْمَٰنِ الرَّحِيمِ", "Ar-raḥmānir-raḥīm(i).", "Yang Maha Pengasih, Maha Penyayang."),
        ayah(1, "Al-Fatihah", 4, "مَالِكِ يَوْمِ الدِّينِ", "Māliki yaumid-dīn(i).", "Pemilik hari pembalasan."),
        ayah(1, "Al-Fatihah", 5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "Iyyāka na'budu wa iyyāka nasta'īn(u).", "Hanya kepada Engkaulah kami menyembah dan hanya kepada Engkaulah kami mohon pertolongan."),
        ayah(1, "Al-Fatihah", 6, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "Ihdinaṣ-ṣirāṭal-mustaqīm(a).", "Tunjukilah kami jalan yang lurus,"),
        ayah(1, "Al-Fatihah", 7, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", "Ṣirāṭal-lażīna an'amta 'alaihim gairil-magḍūbi 'alaihim wa laḍ-ḍāllīn(a).", "(yaitu) jalan orang-orang yang telah Engkau beri nikmat kepadanya; bukan (jalan) mereka yang dimurkai, dan bukan (pula jalan) mereka yang sesat."),

        // Al-Ikhlas (112)
        ayah(112, "Al-Ikhlas", 1, "قُلْ هُوَ اللَّهُ أَحَدٌ", "Qul huwallāhu aḥad(un).", "Katakanlah (Nabi Muhammad), \"Dialah Allah, Yang Maha Esa."),
        ayah(112, "Al-Ikhlas", 2, "اللَّهُ الصَّمَدُ", "Allāhuṣ-ṣamad(u).", "Allah tempat meminta segala sesuatu."),
        ayah(112, "Al-Ikhlas", 3, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "Lam yalid wa lam yūlad.", "(Allah) tidak beranak dan tidak pula diperanakkan."),
        ayah(112, "Al-Ikhlas", 4, "وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ", "Wa lam yakul lahū kufuwan aḥad(un).", "Serta tidak ada sesuatu pun yang setara dengan Dia.\""),

        // Al-Falaq (113)
        ayah(113, "Al-Falaq", 1, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", "Qul a'ūżu birabbil-falaq(i).", "Katakanlah (Nabi Muhammad), \"Aku berlindung kepada Tuhan yang (menjaga) fajar (subuh)"),
        ayah(113, "Al-Falaq", 2, "مِنْ شَرِّ مَا خَلَقَ", "Min syarri mā khalaq(a).", "dari kejahatan (makhluk yang) Dia ciptakan,"),
        ayah(113, "Al-Falaq", 3, "وَمِنْ شَرِّ غَاسِقٍ إِذَا وَقَبَ", "Wa min syarri gāsiqin iżā waqab(a).", "dari kejahatan malam apabila telah gelap gulita,"),
        ayah(113, "Al-Falaq", 4, "وَمِنْ شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ", "Wa min syarrin-naffāṡāti fil-'uqad(i).", "dari kejahatan perempuan-perempuan (penyihir) yang meniup pada buhul-buhul (talinya),"),
        ayah(113, "Al-Falaq", 5, "وَمِنْ شَرِّ حَاسِدٍ إِذَا حَسَدَ", "Wa min syarri ḥāsidin iżā ḥasad(a).", "dan dari kejahatan orang yang dengki apabila dia dengki.\""),

        // An-Nas (114)
        ayah(114, "An-Nas", 1, "قُلْ أَعُوذُ بِرَبِّ النَّاسِ", "Qul a'ūżu birabbin-nās(i).", "Katakanlah (Nabi Muhammad), \"Aku berlindung kepada Tuhan manusia,"),
        ayah(114, "An-Nas", 2, "مَلِكِ النَّاسِ", "Malikin-nās(i).", "raja manusia,"),
        ayah(114, "An-Nas", 3, "إِلَٰهِ النَّاسِ", "Ilāhin-nās(i).", "sembahan manusia"),
        ayah(114, "An-Nas", 4, "مِنْ شَرِّ الْوَسْوَاسِ الْخَنَّاسِ", "Min syarril-waswāsil-khannās(i).", "dari kejahatan (bisikan) setan yang bersembunyi,"),
        ayah(114, "An-Nas", 5, "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ", "Allażī yuwaswisu fī ṣudūrin-nās(i).", "yang membisikkan (kejahatan) ke dalam dada manusia,"),
        ayah(114, "An-Nas", 6, "مِنَ الْجِنَّةِ وَالنَّاسِ", "Minal-jinnati wan-nās(i).", "dari (golongan) jin dan manusia.\"")
    )
}
