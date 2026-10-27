package com.minecorerp.clinicerp.data.repository

import com.minecorerp.clinicerp.data.local.SessionManager
import com.minecorerp.clinicerp.data.remote.JsonRpcRequest
import com.minecorerp.clinicerp.data.remote.JsonRpcResponse
import com.minecorerp.clinicerp.data.remote.OdooService
import com.minecorerp.clinicerp.data.remote.call
import com.minecorerp.clinicerp.data.remote.models.AuthenticateParams
import com.minecorerp.clinicerp.data.remote.models.CallKwParams
import com.minecorerp.clinicerp.data.remote.models.OdooSession
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url
import javax.inject.Inject
import javax.inject.Singleton

interface OdooAuthService {
    @POST
    suspend fun authenticate(
        @Url url: String,
        @Body request: JsonRpcRequest
    ): Response<JsonRpcResponse>
}

@Singleton
class AuthRepository @Inject constructor(
    private val odooService: OdooService,
    internal val sessionManager: SessionManager,
    private val json: Json,
    private val retrofit: Retrofit
) {
    private val authService = retrofit.create(OdooAuthService::class.java)

    suspend fun listDatabases(baseUrl: String): List<String> {
        val url = "${baseUrl.trimEnd('/')}/web/database/list"
        val request = JsonRpcRequest(params = json.encodeToJsonElement(emptyMap<String, String>()))
        val response = odooService.call(url, request)
        return response.result?.let { json.decodeFromJsonElement<List<String>>(it) } ?: emptyList()
    }

    suspend fun authenticate(baseUrl: String, db: String, login: String, pass: String): Result<OdooSession> {
        val url = "${baseUrl.trimEnd('/')}/web/session/authenticate"
        return try {
            val params = AuthenticateParams(db, login, pass)
            val request = JsonRpcRequest(params = json.encodeToJsonElement(params))
            val response = authService.authenticate(url, request)
            val body = response.body()
            
            if (body?.error != null) {
                Result.failure(Exception(body.error.message))
            } else if (body?.result != null) {
                var session = json.decodeFromJsonElement<OdooSession>(body.result)
                
                // If sid is missing in JSON, try to get it from cookies
                if (session.sessionId == null) {
                    val cookies = response.headers().values("Set-Cookie")
                    val sessionCookie = cookies.find { it.contains("session_id=", ignoreCase = true) }
                    if (sessionCookie != null) {
                        val sid = sessionCookie.substringAfter("session_id=").substringBefore(";")
                        session = session.copy(sessionId = sid)
                    }
                }
                
                sessionManager.saveSession(baseUrl, db, login, session.uid, session.sessionId)

                // Fetch partner_id for the user
                try {
                    val userRequest = JsonRpcRequest(params = json.encodeToJsonElement(
                        CallKwParams("res.users", "read", listOf(
                            JsonArray(listOf(JsonPrimitive(session.uid))),
                            JsonArray(listOf(JsonPrimitive("partner_id")))
                        ))
                    ))
                    val userResponse = odooService.call(baseUrl + "/web/dataset/call_kw", userRequest)
                    userResponse.result?.let {
                        val userData = json.decodeFromJsonElement<List<JsonObject>>(it)
                        userData.firstOrNull()?.get("partner_id")?.let { pid ->
                            val id = (pid as? JsonArray)?.get(0)?.jsonPrimitive?.intOrNull
                            if (id != null) sessionManager.savePartnerId(id)
                        }
                    }
                } catch (e: Exception) {
                    // Log but don't fail login
                }

                Result.success(session)
            } else {
                Result.failure(Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserDetails(): Result<com.minecorerp.clinicerp.data.remote.models.ResUser> {
        val userUid = sessionManager.userUid.first() ?: return Result.failure(Exception("Not logged in"))
        val baseUrl = sessionManager.serverUrl.first() ?: ""
        
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("res.users", "read", listOf(
                JsonArray(listOf(JsonPrimitive(userUid))),
                JsonArray(listOf(JsonPrimitive("id"), JsonPrimitive("name"), JsonPrimitive("email")))
            ))
        ))
        
        return try {
            val response = odooService.call(baseUrl + "/web/dataset/call_kw", request)
            val result = response.result?.let { 
                val userData = json.decodeFromJsonElement<List<JsonObject>>(it)
                userData.firstOrNull()?.let { userObj ->
                    val id = userObj["id"]?.jsonPrimitive?.int ?: 0
                    val name = userObj["name"]?.jsonPrimitive?.content ?: ""
                    val email = userObj["email"]?.jsonPrimitive?.contentOrNull
                    com.minecorerp.clinicerp.data.remote.models.ResUser(id, name, email?.takeUnless { it == "false" }) 
                }
            }
            if (result != null) Result.success(result) else Result.failure(Exception("Failed to load user"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCurrentUserDetails(name: String, email: String): Boolean {
        val userUid = sessionManager.userUid.first() ?: return false
        val baseUrl = sessionManager.serverUrl.first() ?: ""
        
        val args = listOf(
            JsonArray(listOf(JsonPrimitive(userUid))),
            buildJsonObject {
                put("name", name)
                put("email", email)
            }
        )
        
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("res.users", "write", args)
        ))
        
        return try {
            val response = odooService.call(baseUrl + "/web/dataset/call_kw", request)
            json.decodeFromJsonElement<Boolean>(response.result ?: JsonPrimitive(false))
        } catch (e: Exception) {
            false
        }
    }

    suspend fun changePassword(oldPass: String, newPass: String): Result<Boolean> {
        val userUid = sessionManager.userUid.first() ?: return Result.failure(Exception("Not logged in"))
        val baseUrl = sessionManager.serverUrl.first() ?: ""
        
        val args = listOf(JsonPrimitive(oldPass), JsonPrimitive(newPass))
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("res.users", "change_password", args)
        ))
        
        return try {
            val response = odooService.call(baseUrl.trimEnd('/') + "/web/dataset/call_kw", request)
            val failure = (response.result as? JsonObject)?.get("error")
            if (failure != null && failure != JsonNull) return Result.failure(Exception(failure.toString()))
            if (response.error != null) {
                Result.failure(Exception(response.error.message))
            } else {
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        sessionManager.clearSession()
    }
}
