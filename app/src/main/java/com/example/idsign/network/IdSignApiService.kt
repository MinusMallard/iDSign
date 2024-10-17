package com.example.idsign.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface IdSignApiService {

    @GET("hash/{emailId}")
    suspend fun getHash(
        @Path("emailId") emailId: String
    ): String

    @POST("private")
    suspend fun getPrivateHash(
        @Body publicKey: String
    ): String

}