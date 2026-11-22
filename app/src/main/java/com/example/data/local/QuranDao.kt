package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {

    @Query("SELECT * FROM surahs ORDER BY nomor ASC")
    fun observeAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE nomor = :nomor LIMIT 1")
    fun observeSurah(nomor: Int): Flow<SurahEntity?>

    @Query("SELECT * FROM surahs WHERE nomor = :nomor LIMIT 1")
    suspend fun getSurah(nomor: Int): SurahEntity?

    @Query("SELECT COUNT(*) FROM surahs")
    suspend fun getSurahCount(): Int

    @Query("SELECT COUNT(*) FROM surahs WHERE isDownloaded = 1")
    fun observeDownloadedSurahCount(): Flow<Int>

    @Query("SELECT nomor FROM surahs WHERE isDownloaded = 0 ORDER BY nomor ASC")
    suspend fun getUndownloadedSurahNumbers(): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(surahs: List<SurahEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurah(surah: SurahEntity)

    @Query("SELECT * FROM ayahs WHERE surahNomor = :surahNomor ORDER BY nomorAyat ASC")
    fun observeAyahsBySurah(surahNomor: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM ayahs WHERE surahNomor = :surahNomor ORDER BY nomorAyat ASC")
    suspend fun getAyahsBySurah(surahNomor: Int): List<AyahEntity>

    @Query("SELECT COUNT(*) FROM ayahs")
    fun observeTotalAyahCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<AyahEntity>)

    @Query(
        """
        SELECT * FROM ayahs
        WHERE teksIndonesia LIKE '%' || :query || '%'
           OR teksLatin LIKE '%' || :query || '%'
           OR teksArab LIKE '%' || :query || '%'
           OR surahNamaLatin LIKE '%' || :query || '%'
        ORDER BY surahNomor ASC, nomorAyat ASC
        LIMIT 80
        """
    )
    suspend fun searchAyahsOffline(query: String): List<AyahEntity>

    @Query(
        """
        SELECT * FROM surahs
        WHERE namaLatin LIKE '%' || :query || '%'
           OR arti LIKE '%' || :query || '%'
           OR namaArab LIKE '%' || :query || '%'
           OR CAST(nomor AS TEXT) = :query
        ORDER BY nomor ASC
        """
    )
    suspend fun searchSurahsOffline(query: String): List<SurahEntity>

    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun observeBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE id = :id)")
    suspend fun isBookmarked(id: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Int)
}
