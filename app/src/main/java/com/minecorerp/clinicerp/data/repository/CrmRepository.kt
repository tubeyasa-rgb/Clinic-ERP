package com.minecorerp.clinicerp.data.repository

import com.minecorerp.clinicerp.data.local.SessionManager
import com.minecorerp.clinicerp.data.remote.JsonRpcRequest
import com.minecorerp.clinicerp.data.remote.OdooService
import com.minecorerp.clinicerp.data.remote.call
import com.minecorerp.clinicerp.data.remote.models.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrmRepository @Inject constructor(
    private val odooService: OdooService,
    private val sessionManager: SessionManager,
    private val json: Json,
    private val retrofit: retrofit2.Retrofit,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {
    private suspend fun getBaseUrl() = sessionManager.serverUrl.first() ?: ""

    suspend fun getStages(): List<CrmStage> {
        val kwargs = mapOf("order" to JsonPrimitive("sequence asc"))
        val args = listOf(JsonArray(emptyList()), JsonArray(listOf("id", "name", "sequence").map { JsonPrimitive(it) }))
        val baseUrl = getBaseUrl()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("crm.stage", "search_read", args, kwargs)
        ))
        return try {
            val response = odooService.call(url, request)
            response.result?.let { json.decodeFromJsonElement<List<CrmStage>>(it) } ?: emptyList()
        } catch (e: Exception) { throw e }
    }

    suspend fun getLeads(): List<CrmLead> {
        val kwargs = mapOf("order" to JsonPrimitive("priority desc, create_date desc"), "limit" to JsonPrimitive(200))
        val domain = listOf(
            JsonPrimitive("|"),
            JsonArray(listOf(JsonPrimitive("type"), JsonPrimitive("="), JsonPrimitive("opportunity"))),
            JsonArray(listOf(JsonPrimitive("type"), JsonPrimitive("="), JsonPrimitive("lead")))
        )
        val fields = listOf(
            "id", "name", "contact_name", "phone", "email_from", "stage_id",
            "expected_revenue", "create_date", "description", "date_deadline",
            "user_id", "partner_id", "tag_ids", "priority", "team_id", "probability",
            "campaign_id", "medium_id", "source_id"
        ).map { JsonPrimitive(it) }
        
        val baseUrl = getBaseUrl()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        val records = mutableListOf<CrmLead>()
        var offset = 0
        while (true) {
            val pageKwargs = kwargs + mapOf("offset" to JsonPrimitive(offset))
            val request = JsonRpcRequest(params = json.encodeToJsonElement(
                CallKwParams("crm.lead", "search_read", listOf(JsonArray(domain), JsonArray(fields)), pageKwargs)
            ))
            val response = odooService.call(url, request)
            val page = response.result?.let { json.decodeFromJsonElement<List<CrmLead>>(it) } ?: emptyList()
            records.addAll(page)
            if (page.size < 200) break
            offset += page.size
        }
        return records.distinctBy { it.id }
    }

    suspend fun getTeams(): List<CrmTeam> {
        val baseUrl = getBaseUrl()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("crm.team", "search_read", listOf(JsonArray(emptyList()), JsonArray(listOf(JsonPrimitive("id"), JsonPrimitive("name")))))
        ))
        return try {
            val response = odooService.call(url, request)
            response.result?.let { json.decodeFromJsonElement<List<CrmTeam>>(it) } ?: emptyList()
        } catch (e: Exception) { throw e }
    }

    suspend fun getTags(): List<CrmTag> {
        val baseUrl = getBaseUrl()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        val fields = listOf(JsonPrimitive("id"), JsonPrimitive("name"))
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("crm.tag", "search_read", listOf(JsonArray(emptyList()), JsonArray(fields)))
        ))
        return try {
            val response = odooService.call(url, request)
            response.result?.let { json.decodeFromJsonElement<List<CrmTag>>(it) } ?: emptyList()
        } catch (e: Exception) { throw e }
    }

    suspend fun getUsers(): List<ResUser> {
        val baseUrl = getBaseUrl()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        val domain = listOf(JsonArray(listOf(JsonPrimitive("active"), JsonPrimitive("="), JsonPrimitive(true))))
        val fields = listOf(JsonPrimitive("id"), JsonPrimitive("name"))
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("res.users", "search_read", listOf(JsonArray(domain), JsonArray(fields)))
        ))
        return try {
            val response = odooService.call(url, request)
            response.result?.let { json.decodeFromJsonElement<List<ResUser>>(it) } ?: emptyList()
        } catch (e: Exception) { throw e }
    }

    suspend fun getUtmSources(): List<UtmSource> = fetchUtm("utm.source")
    suspend fun getUtmMediums(): List<UtmMedium> = fetchUtm("utm.medium")
    suspend fun getUtmCampaigns(): List<UtmCampaign> = fetchUtm("utm.campaign")

    private suspend inline fun <reified T> fetchUtm(model: String): List<T> {
        val baseUrl = getBaseUrl()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams(model, "search_read", listOf(JsonArray(emptyList()), JsonArray(listOf(JsonPrimitive("id"), JsonPrimitive("name")))))
        ))
        return try {
            val response = odooService.call(url, request)
            // This is a bit tricky with generic T, but since they all have id/name, it's usually safe if called correctly.
            response.result?.let { json.decodeFromJsonElement(it) } ?: emptyList()
        } catch (e: Exception) { throw e }
    }

    suspend fun getPartners(): List<ResUser> { 
        val baseUrl = getBaseUrl()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        // Simplified domain: just fetch active partners, and removed image_1920 for performance
        val domain = listOf(JsonArray(listOf(JsonPrimitive("active"), JsonPrimitive("="), JsonPrimitive(true))))
        val fields = listOf("id", "name", "email", "phone").map { JsonPrimitive(it) }
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("res.partner", "search_read", listOf(JsonArray(domain), JsonArray(fields)))
        ))
        return try {
            val response = odooService.call(url, request)
            if (response.error != null) {
                android.util.Log.e("OdooRPC", "Partner Fetch Error: ${response.error.message}")
                emptyList()
            } else {
                response.result?.let { json.decodeFromJsonElement<List<ResUser>>(it) } ?: emptyList()
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getModelId(modelName: String): Int? {
        val baseUrl = getBaseUrl()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        val domain = listOf(JsonArray(listOf(JsonPrimitive("model"), JsonPrimitive("="), JsonPrimitive(modelName))))
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("ir.model", "search", listOf(JsonArray(domain)))
        ))
        return try {
            val response = odooService.call(url, request)
            response.result?.let { json.decodeFromJsonElement<List<Int>>(it).firstOrNull() }
        } catch (e: Exception) { throw e }
    }

    suspend fun checkPartnerPermissions(): Map<String, Boolean> {
        val baseUrl = getBaseUrl()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        return try {
            val canWrite = try {
                val req = JsonRpcRequest(params = json.encodeToJsonElement(CallKwParams("res.partner", "has_access", listOf(JsonArray(emptyList()), JsonPrimitive("write")))))
                odooService.call(url, req).result?.jsonPrimitive?.boolean ?: false
            } catch (e: Exception) { false }
            val canCreate = try {
                val req = JsonRpcRequest(params = json.encodeToJsonElement(CallKwParams("res.partner", "has_access", listOf(JsonArray(emptyList()), JsonPrimitive("create")))))
                odooService.call(url, req).result?.jsonPrimitive?.boolean ?: false
            } catch (e: Exception) { false }
            mapOf("create" to canCreate, "write" to canWrite)
        } catch (e: Exception) { mapOf("create" to false, "write" to false) }
    }

    suspend fun createLead(
        name: String, contactName: String, phone: String, email: String, 
        description: String, userId: Int?, tagIds: List<Int>, partnerId: Int?, 
        priority: String, teamId: Int?, probability: Double?,
        campaignId: Int?, mediumId: Int?, sourceId: Int?
    ): Int? {
        val args = listOf(
            buildJsonObject {
                put("name", name)
                if (partnerId != null) put("partner_id", partnerId) else put("contact_name", contactName)
                put("phone", phone)
                put("email_from", email)
                put("description", description)
                put("priority", priority)
                put("type", "opportunity")
                if (userId != null) put("user_id", userId)
                if (teamId != null) put("team_id", teamId)
                if (probability != null) put("probability", probability)
                if (campaignId != null) put("campaign_id", campaignId)
                if (mediumId != null) put("medium_id", mediumId)
                if (sourceId != null) put("source_id", sourceId)
                put("tag_ids", JsonArray(listOf(JsonArray(listOf(JsonPrimitive(6), JsonPrimitive(0), JsonArray(tagIds.map { JsonPrimitive(it) }))))))
            }
        )
        val baseUrl = getBaseUrl()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        val request = JsonRpcRequest(params = json.encodeToJsonElement(CallKwParams("crm.lead", "create", args)))
        return try {
            val response = odooService.call(url, request)
            response.result?.jsonPrimitive?.intOrNull
        } catch (e: Exception) { throw e }
    }
    
    suspend fun updateLead(
        leadId: Int, name: String, contactName: String, phone: String, email: String, 
        description: String, stageId: Int, userId: Int?, tagIds: List<Int>, 
        partnerId: Int?, priority: String, teamId: Int?, probability: Double?,
        campaignId: Int?, mediumId: Int?, sourceId: Int?
    ): Boolean {
        val args = listOf(
            JsonArray(listOf(JsonPrimitive(leadId))),
            buildJsonObject {
                put("name", name)
                if (partnerId != null) put("partner_id", partnerId) else put("contact_name", contactName)
                put("phone", phone)
                put("email_from", email)
                put("description", description)
                put("priority", priority)
                put("stage_id", stageId)
                if (userId != null) put("user_id", userId)
                if (teamId != null) put("team_id", teamId)
                if (probability != null) put("probability", probability)
                if (campaignId != null) put("campaign_id", campaignId)
                if (mediumId != null) put("medium_id", mediumId)
                if (sourceId != null) put("source_id", sourceId)
                put("tag_ids", JsonArray(listOf(JsonArray(listOf(JsonPrimitive(6), JsonPrimitive(0), JsonArray(tagIds.map { JsonPrimitive(it) }))))))
            }
        )
        val baseUrl = getBaseUrl()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        val request = JsonRpcRequest(params = json.encodeToJsonElement(CallKwParams("crm.lead", "write", args)))
        return try {
            val response = odooService.call(url, request)
            response.result?.jsonPrimitive?.boolean ?: false
        } catch (e: Exception) { throw e }
    }

    suspend fun getAttachments(resModel: String, resId: Int): List<OdooAttachment> {
        val baseUrl = getBaseUrl()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        val domain = listOf(
            JsonArray(listOf(JsonPrimitive("res_model"), JsonPrimitive("="), JsonPrimitive(resModel))),
            JsonArray(listOf(JsonPrimitive("res_id"), JsonPrimitive("="), JsonPrimitive(resId))),
            JsonArray(listOf(JsonPrimitive("res_field"), JsonPrimitive("="), JsonPrimitive(false))) // Only chatter attachments
        )
        val fields = listOf("id", "name", "mimetype", "create_date", "file_size").map { JsonPrimitive(it) }
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("ir.attachment", "search_read", listOf(JsonArray(domain), JsonArray(fields)))
        ))
        return try {
            val response = odooService.call(url, request)
            response.result?.let { json.decodeFromJsonElement<List<OdooAttachment>>(it) } ?: emptyList()
        } catch (e: Exception) { throw e }
    }

    suspend fun downloadAttachment(id: Int, fileName: String): java.io.File = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val directory = java.io.File(context.cacheDir, "attachments").apply { mkdirs() }
        val safeName = fileName.replace(Regex("[^\\p{L}\\p{N}._-]"), "_").take(120).ifBlank { "attachment" }
        val target = java.io.File(directory, "${id}_$safeName")
        try {
            val service = retrofit.create(com.minecorerp.clinicerp.data.remote.OdooFileService::class.java)
            service.download("${getBaseUrl().trimEnd('/')}/web/content/$id?download=true").use { body ->
                require(body.contentLength() <= 30L * 1024 * 1024) { "المرفق أكبر من 30 ميجابايت" }
                body.byteStream().use { input -> target.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        total += read
                        require(total <= 30L * 1024 * 1024) { "المرفق أكبر من 30 ميجابايت" }
                        output.write(buffer, 0, read)
                    }
                } }
            }
            target
        } catch (e: Exception) { target.delete(); throw e }
    }

    suspend fun uploadAttachment(resModel: String, resId: Int, fileName: String, base64Data: String): Int? {
        val baseUrl = getBaseUrl()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        val args = listOf(buildJsonObject {
            put("name", fileName)
            put("datas", base64Data)
            put("res_model", resModel)
            put("res_id", resId)
            put("type", "binary")
        })
        val request = JsonRpcRequest(params = json.encodeToJsonElement(CallKwParams("ir.attachment", "create", args)))
        return try {
            val response = odooService.call(url, request)
            response.result?.jsonPrimitive?.intOrNull
        } catch (e: Exception) { throw e }
    }

    suspend fun createPartner(name: String, phone: String, email: String): Int? {
        val baseUrl = getBaseUrl()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        val args = listOf(
            buildJsonObject {
                put("name", name)
                put("phone", phone)
                put("email", email)
            }
        )
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("res.partner", "create", args)
        ))
        return try {
            val response = odooService.call(url, request)
            if (response.error != null) {
                android.util.Log.e("OdooRPC", "Partner Create Error: ${response.error.message}")
                null
            } else {
                response.result?.jsonPrimitive?.intOrNull
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updatePartner(id: Int, name: String, phone: String, email: String): Boolean {
        val baseUrl = getBaseUrl()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        val args = listOf(JsonArray(listOf(JsonPrimitive(id))), buildJsonObject { put("name", name); put("phone", phone); put("email", email) })
        val request = JsonRpcRequest(params = json.encodeToJsonElement(CallKwParams("res.partner", "write", args)))
        return try {
            val response = odooService.call(url, request)
            response.result?.jsonPrimitive?.boolean ?: false
        } catch (e: Exception) { throw e }
    }

    suspend fun deletePartner(id: Int): Boolean {
        val baseUrl = getBaseUrl()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        val request = JsonRpcRequest(params = json.encodeToJsonElement(CallKwParams("res.partner", "unlink", listOf(JsonArray(listOf(JsonPrimitive(id)))))))
        return try {
            val response = odooService.call(url, request)
            response.result?.jsonPrimitive?.boolean ?: false
        } catch (e: Exception) { throw e }
    }
}
