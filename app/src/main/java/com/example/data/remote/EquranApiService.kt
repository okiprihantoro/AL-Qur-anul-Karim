package com.example.data.remote

import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class EquranSurahDetailResponse(
    val code: Int,
    val message: String,
    val data: EquranSurahDetailData?
)

@JsonClass(generateAdapter = true)
data class EquranSurahDetailData(
    val nomor: Int,
    val nama: String,
    val namaLatin: String,
    val jumlahAyat: Int,
    val tempatTurun: String,
    val arti: String,
    val deskripsi: String,
    val audioFull: Map<String, String>?,
    val ayat: List<EquranAyahDto>?
)

@JsonClass(generateAdapter = true)
data class EquranAyahDto(
    val nomorAyat: Int,
    val teksArab: String,
    val teksLatin: String,
    val teksIndonesia: String,
    val audio: Map<String, String>?
)

interface EquranApiService {
    @GET("api/v2/surat/{nomor}")
    suspend fun getSurahDetail(@Path("nomor") nomor: Int): EquranSurahDetailResponse

    companion object {
        private const val BASE_URL = "https://equran.id/"

        fun create(): EquranApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(EquranApiService::class.java)
        }
    }
}
