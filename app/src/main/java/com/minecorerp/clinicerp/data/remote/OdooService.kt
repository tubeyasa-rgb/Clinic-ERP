package com.minecorerp.clinicerp.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.POST
import retrofit2.http.Url

import kotlinx.serialization.json.JsonElement

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val method: String = "call",
    val params: JsonElement,
    val id: Int = (0..1000).random()
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String,
    val id: Int,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonRpcErrorData? = null
)

@Serializable
data class JsonRpcErrorData(
    val name: String,
    val debug: String? = null,
    val message: String? = null,
    val arguments: JsonElement? = null,
    val exception_type: String? = null
)

interface OdooService {
    @POST
    suspend fun rawCall(
        @Url url: String,
        @Body request: JsonRpcRequest
    ): JsonRpcResponse
}


suspend fun OdooService.call(url: String, request: JsonRpcRequest): JsonRpcResponse {
    val response = rawCall(url, request)
    response.error?.let { throw IllegalStateException(it.data?.message ?: it.message) }
    return response
}

interface OdooFileService {
    @GET @Streaming
    suspend fun download(@Url url: String): okhttp3.ResponseBody
}
