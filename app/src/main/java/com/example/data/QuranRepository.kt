package com.example.data

import android.content.Context
import com.example.data.local.AyahEntity
import com.example.data.local.BookmarkEntity
import com.example.data.local.QuranDao
import com.example.data.local.SurahEntity
import com.example.data.remote.EquranApiService
import com.example.data.seed.BundledQuranSeeder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class OfflineSyncProgress(
    val isSyncing: Boolean = false,
    val completedCount: Int = 0,
    val totalCount: Int = 114,
    val currentSurahName: String = "",
    val errorMessage: String? = null
)

class QuranRepository(
    private val context: Context,
    private val dao: QuranDao,
    private val api: EquranApiService
) {
    val allSurahs: Flow<List<SurahEntity>> = dao.observeAllSurahs()
    val downloadedSurahCount: Flow<Int> = dao.observeDownloadedSurahCount()
    val totalAyahCount: Flow<Int> = dao.observeTotalAyahCount()
    val bookmarks: Flow<List<BookmarkEntity>> = dao.observeBookmarks()

    private val _syncProgress = MutableStateFlow(OfflineSyncProgress())
    val syncProgress: StateFlow<OfflineSyncProgress> = _syncProgress.asStateFlow()

    @Volatile
    private var cancelRequested = false

    suspend fun initializeDatabase() {
        withContext(Dispatchers.IO) {
            BundledQuranSeeder.seedIfEmpty(context, dao)
        }
    }

    fun observeSurah(nomor: Int): Flow<SurahEntity?> = dao.observeSurah(nomor)

    fun observeAyahs(surahNomor: Int): Flow<List<AyahEntity>> = dao.observeAyahsBySurah(surahNomor)

    suspend fun ensureSurahLoaded(surahNomor: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val existingAyahs = dao.getAyahsBySurah(surahNomor)
        val surah = dao.getSurah(surahNomor)
        if (surah?.isDownloaded == true && existingAyahs.isNotEmpty()) {
            return@withContext Result.success(Unit)
        }
        fetchAndStoreSurah(surahNomor)
    }

    suspend fun fetchAndStoreSurah(surahNomor: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.getSurahDetail(surahNomor)
            val data = response.data ?: return@withContext Result.failure(
                IllegalStateException("Data surat tidak ditemukan dari server.")
            )

            val cleanDesc = data.deskripsi
                .replace(Regex("<[^>]*>"), " ")
                .replace(Regex("\\s+"), " ")
                .trim()

            val surahEntity = SurahEntity(
                nomor = data.nomor,
                namaArab = data.nama,
                namaLatin = data.namaLatin,
                jumlahAyat = data.jumlahAyat,
                tempatTurun = data.tempatTurun,
                arti = data.arti,
                deskripsi = cleanDesc,
                audioFull01 = data.audioFull?.get("01").orEmpty(),
                audioFull02 = data.audioFull?.get("02").orEmpty(),
                audioFull03 = data.audioFull?.get("03").orEmpty(),
                audioFull04 = data.audioFull?.get("04").orEmpty(),
                audioFull05 = data.audioFull?.get("05").orEmpty(),
                audioFull06 = data.audioFull?.get("06").orEmpty(),
                isDownloaded = true
            )

            val ayahs = data.ayat.orEmpty().map { dto ->
                AyahEntity(
                    id = data.nomor * 1000 + dto.nomorAyat,
                    surahNomor = data.nomor,
                    surahNamaLatin = data.namaLatin,
                    nomorAyat = dto.nomorAyat,
                    teksArab = dto.teksArab,
                    teksLatin = dto.teksLatin,
                    teksIndonesia = dto.teksIndonesia,
                    audio01 = dto.audio?.get("01") ?: QariCatalog.buildFallbackAyahAudioUrl("01", data.nomor, dto.nomorAyat),
                    audio02 = dto.audio?.get("02") ?: QariCatalog.buildFallbackAyahAudioUrl("02", data.nomor, dto.nomorAyat),
                    audio03 = dto.audio?.get("03") ?: QariCatalog.buildFallbackAyahAudioUrl("03", data.nomor, dto.nomorAyat),
                    audio04 = dto.audio?.get("04") ?: QariCatalog.buildFallbackAyahAudioUrl("04", data.nomor, dto.nomorAyat),
                    audio05 = dto.audio?.get("05") ?: QariCatalog.buildFallbackAyahAudioUrl("05", data.nomor, dto.nomorAyat),
                    audio06 = dto.audio?.get("06") ?: QariCatalog.buildFallbackAyahAudioUrl("06", data.nomor, dto.nomorAyat)
                )
            }

            dao.insertSurah(surahEntity)
            dao.insertAyahs(ayahs)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadAllRemainingSurahs() = withContext(Dispatchers.IO) {
        if (_syncProgress.value.isSyncing) return@withContext
        cancelRequested = false
        val remaining = dao.getUndownloadedSurahNumbers()
        if (remaining.isEmpty()) {
            _syncProgress.value = OfflineSyncProgress(
                isSyncing = false,
                completedCount = 114,
                totalCount = 114,
                currentSurahName = "Lengkap"
            )
            return@withContext
        }

        val alreadyDownloaded = 114 - remaining.size
        var completed = alreadyDownloaded
        _syncProgress.value = OfflineSyncProgress(
            isSyncing = true,
            completedCount = completed,
            totalCount = 114,
            currentSurahName = "Memulai unduhan..."
        )

        val batches = remaining.chunked(4)
        for (batch in batches) {
            if (cancelRequested) break
            val results = coroutineScope {
                batch.map { surahNo ->
                    async {
                        val res = fetchAndStoreSurah(surahNo)
                        val name = dao.getSurah(surahNo)?.namaLatin ?: "Surat $surahNo"
                        surahNo to (res.isSuccess to name)
                    }
                }.awaitAll()
            }
            val failed = results.any { !it.second.first }
            results.forEach { (_, pair) ->
                if (pair.first) {
                    completed++
                }
            }
            val lastName = results.lastOrNull()?.second?.second.orEmpty()
            _syncProgress.value = _syncProgress.value.copy(
                completedCount = completed,
                currentSurahName = lastName,
                errorMessage = if (failed) "Sebagian surat gagal diunduh. Periksa koneksi internet Anda." else null
            )
            if (failed && results.all { !it.second.first }) {
                break
            }
        }

        _syncProgress.value = _syncProgress.value.copy(isSyncing = false)
    }

    fun cancelFullDownload() {
        cancelRequested = true
        _syncProgress.value = _syncProgress.value.copy(isSyncing = false)
    }

    suspend fun searchOffline(query: String): Pair<List<SurahEntity>, List<AyahEntity>> =
        withContext(Dispatchers.IO) {
            val clean = query.trim()
            if (clean.isEmpty()) return@withContext emptyList<SurahEntity>() to emptyList()
            val surahs = dao.searchSurahsOffline(clean)
            val ayahs = dao.searchAyahsOffline(clean)
            surahs to ayahs
        }

    suspend fun toggleBookmark(ayah: AyahEntity): Boolean = withContext(Dispatchers.IO) {
        val exists = dao.isBookmarked(ayah.id)
        if (exists) {
            dao.deleteBookmarkById(ayah.id)
            false
        } else {
            dao.insertBookmark(
                BookmarkEntity(
                    id = ayah.id,
                    surahNomor = ayah.surahNomor,
                    surahNamaLatin = ayah.surahNamaLatin,
                    nomorAyat = ayah.nomorAyat,
                    teksArab = ayah.teksArab,
                    teksIndonesia = ayah.teksIndonesia
                )
            )
            true
        }
    }

    suspend fun removeBookmark(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteBookmarkById(id)
    }
}
