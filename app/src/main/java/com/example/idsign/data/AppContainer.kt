package com.example.idsign.data

import android.content.Context
import com.example.idsign.network.IdSignApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

interface AppContainer {
    val idSignDataRespository: IdSignDataRespository
}

class DefaultAppContainer(private val context: Context): AppContainer {

    private val baseUrl = "http://10.60.254.185:8080/"

    private val client = createOkHttpClient()

    private val retrofit = Retrofit.Builder()
        .addConverterFactory(ScalarsConverterFactory.create())
        .client(client)
        .baseUrl(baseUrl)
        .build()

    private val retrofitService: IdSignApiService by lazy {
        retrofit.create(IdSignApiService::class.java)
    }

    override val idSignDataRespository: IdSignDataRespository by lazy {
        IdSignDataRepositoryImpl(retrofitService)
    }
}

fun createOkHttpClient(): OkHttpClient {
    val logging = HttpLoggingInterceptor()
    logging.setLevel(HttpLoggingInterceptor.Level.BODY)

    return OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()
}