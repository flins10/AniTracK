package com.example.data.remote

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class FlexibleString

class FlexibleStringAdapter {
    @FromJson
    @FlexibleString
    fun fromJson(reader: JsonReader): String? {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> reader.nextString()
            JsonReader.Token.NUMBER -> reader.nextString()
            JsonReader.Token.BOOLEAN -> reader.nextBoolean().toString()
            JsonReader.Token.BEGIN_ARRAY -> {
                val list = mutableListOf<String>()
                reader.beginArray()
                while (reader.hasNext()) {
                    when (reader.peek()) {
                        JsonReader.Token.STRING -> list.add(reader.nextString())
                        JsonReader.Token.NUMBER -> list.add(reader.nextString())
                        else -> reader.skipValue()
                    }
                }
                reader.endArray()
                list.joinToString("; ")
            }
            JsonReader.Token.NULL -> reader.nextNull()
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, @FlexibleString value: String?) {
        writer.value(value)
    }
}

data class ArchiveSearchResponse(
    val responseHeader: ArchiveResponseHeader? = null,
    val response: ArchiveSearchBody? = null
)

data class ArchiveResponseHeader(
    val status: Int? = null,
    val QTime: Int? = null
)

data class ArchiveSearchBody(
    val numFound: Int? = null,
    val start: Int? = null,
    val docs: List<ArchiveDocItem>? = emptyList()
)

data class ArchiveDocItem(
    val identifier: String,
    @FlexibleString val title: String? = null,
    @FlexibleString val description: String? = null,
    @FlexibleString val creator: String? = null,
    @FlexibleString val year: String? = null,
    @FlexibleString val mediatype: String? = null,
    @FlexibleString val subject: String? = null,
    @FlexibleString val licenseurl: String? = null,
    @FlexibleString val publicdate: String? = null,
    @FlexibleString val item_size: String? = null
)

data class ArchiveMetadataResponse(
    val created: Long? = null,
    val d1: String? = null,
    val d2: String? = null,
    val dir: String? = null,
    val files: List<ArchiveFileItem>? = emptyList(),
    val files_count: Int? = null,
    val item_size: Long? = null,
    val metadata: ArchiveItemMetadata? = null,
    val server: String? = null
)

data class ArchiveItemMetadata(
    val identifier: String? = null,
    @FlexibleString val title: String? = null,
    @FlexibleString val description: String? = null,
    @FlexibleString val creator: String? = null,
    @FlexibleString val year: String? = null,
    @FlexibleString val licenseurl: String? = null,
    @FlexibleString val mediatype: String? = null,
    @FlexibleString val subject: String? = null,
    @FlexibleString val publicdate: String? = null
)

data class ArchiveFileItem(
    val name: String,
    val source: String? = null,
    val format: String? = null,
    val size: String? = null,
    val length: String? = null,
    val title: String? = null,
    val height: String? = null,
    val width: String? = null,
    val original: String? = null
)

interface InternetArchiveApiService {
    @GET("advancedsearch.php")
    suspend fun searchArchive(
        @Query("q") query: String,
        @Query("fl[]") fields: List<String> = listOf("identifier", "title", "description", "creator", "year", "mediatype", "subject", "licenseurl", "publicdate", "item_size"),
        @Query("rows") rows: Int = 20,
        @Query("page") page: Int = 1,
        @Query("output") output: String = "json"
    ): Response<ArchiveSearchResponse>

    @GET("metadata/{identifier}")
    suspend fun getItemMetadata(
        @Path("identifier") identifier: String
    ): Response<ArchiveMetadataResponse>

    companion object {
        const val BASE_URL = "https://archive.org/"

        fun create(): InternetArchiveApiService {
            val moshi = Moshi.Builder()
                .add(FlexibleStringAdapter())
                .addLast(KotlinJsonAdapterFactory())
                .build()

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(InternetArchiveApiService::class.java)
        }

        fun getThumbnailUrl(identifier: String): String {
            return "https://archive.org/services/img/$identifier"
        }

        fun getDirectDownloadUrl(identifier: String, filename: String): String {
            val encodedPath = filename.split("/").joinToString("/") { segment ->
                URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
            }
            return "https://archive.org/download/$identifier/$encodedPath"
        }
    }
}
