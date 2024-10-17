package com.example.idsign.data

import com.example.idsign.network.IdSignApiService

interface IdSignDataRespository {

    suspend fun getHash(emailId: String) : String

    suspend fun getPrivateHash(public: String): String
}

class IdSignDataRepositoryImpl(private val apiService: IdSignApiService) : IdSignDataRespository {

    override suspend fun getHash(emailId: String): String = apiService.getHash(emailId)

    override suspend fun getPrivateHash(public: String): String = apiService.getPrivateHash(public)
}