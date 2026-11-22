package com.example.data

data class QariInfo(
    val id: String,
    val name: String,
    val style: String,
    val everyAyahFolder: String
)

object QariCatalog {
    val qaris = listOf(
        QariInfo("01", "Syekh Mishari Rashid Al-Afasy", "Murattal Merdu (Kuwait)", "Alafasy_128kbps"),
        QariInfo("02", "Syekh Abdurrahman As-Sudais", "Imam Masjidil Haram", "Abdurrahmaan_As-Sudais_192kbps"),
        QariInfo("03", "Syekh Abu Bakr Ash-Shatri", "Murattal Tartil", "Abu_Bakr_Ash-Shaatree_128kbps"),
        QariInfo("04", "Syekh Hani Ar-Rifai", "Murattal Tenang", "Hani_Rifai_192kbps"),
        QariInfo("05", "Syekh Mahmoud Khalil Al-Husary", "Mujawwad / Belajar Tajwid", "Husary_128kbps"),
        QariInfo("06", "Syekh Muhammad Siddiq Al-Minshawi", "Klasik Tartil", "Minshawy_Murattal_128kbps")
    )

    fun findById(id: String): QariInfo = qaris.firstOrNull { it.id == id } ?: qaris.first()

    fun buildFallbackAyahAudioUrl(qariId: String, surahNomor: Int, ayahNomor: Int): String {
        val folder = findById(qariId).everyAyahFolder
        val s = surahNomor.toString().padStart(3, '0')
        val a = ayahNomor.toString().padStart(3, '0')
        return "https://everyayah.com/data/$folder/$s$a.mp3"
    }
}
