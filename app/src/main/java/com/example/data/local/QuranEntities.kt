package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val nomor: Int,
    val namaArab: String,
    val namaLatin: String,
    val jumlahAyat: Int,
    val tempatTurun: String,
    val arti: String,
    val deskripsi: String = "",
    val audioFull01: String = "",
    val audioFull02: String = "",
    val audioFull03: String = "",
    val audioFull04: String = "",
    val audioFull05: String = "",
    val audioFull06: String = "",
    val isDownloaded: Boolean = false
)

@Entity(
    tableName = "ayahs",
    indices = [
        Index(value = ["surahNomor", "nomorAyat"], unique = true)
    ]
)
data class AyahEntity(
    @PrimaryKey val id: Int, // surahNomor * 1000 + nomorAyat
    val surahNomor: Int,
    val surahNamaLatin: String,
    val nomorAyat: Int,
    val teksArab: String,
    val teksLatin: String,
    val teksIndonesia: String,
    val audio01: String = "",
    val audio02: String = "",
    val audio03: String = "",
    val audio04: String = "",
    val audio05: String = "",
    val audio06: String = ""
) {
    fun audioForQari(qariId: String): String = when (qariId) {
        "01" -> audio01
        "02" -> audio02
        "03" -> audio03
        "04" -> audio04
        "05" -> audio05
        "06" -> audio06
        else -> audio01
    }
}

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: Int, // surahNomor * 1000 + nomorAyat
    val surahNomor: Int,
    val surahNamaLatin: String,
    val nomorAyat: Int,
    val teksArab: String,
    val teksIndonesia: String,
    val createdAt: Long = System.currentTimeMillis()
)
