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
class ActivityRepository @Inject constructor(
    private val odooService: OdooService,
    private val sessionManager: SessionManager,
    private val json: Json
) {
    private suspend fun getBaseUrl() = sessionManager.serverUrl.first() ?: ""

    suspend fun getMyActivities(): List<MailActivity> {
        val baseUrl = getBaseUrl()
        if (baseUrl.isBlank()) return emptyList()
        
        val userUid = sessionManager.userUid.first() ?: return emptyList()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        
        val domain = listOf(
            JsonArray(listOf(JsonPrimitive("user_id"), JsonPrimitive("="), JsonPrimitive(userUid))),
            JsonArray(listOf(JsonPrimitive("state"), JsonPrimitive("!="), JsonPrimitive("done")))
        )
        val fields = listOf("id", "res_id", "res_model", "res_name", "activity_type_id", "summary", "note", "date_deadline", "user_id").map { JsonPrimitive(it) }
        
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("mail.activity", "search_read", listOf(JsonArray(domain), JsonArray(fields)))
        ))
        
        return try {
            val response = odooService.call(url, request)
            response.result?.let { json.decodeFromJsonElement<List<MailActivity>>(it) } ?: emptyList()
        } catch (e: Exception) { throw e }
    }

    suspend fun getActivitiesForLead(leadId: Int): List<MailActivity> {
        val url = "${getBaseUrl().trimEnd('/')}/web/dataset/call_kw"
        val domain = buildJsonArray {
            add(buildJsonArray { add("res_model"); add("="); add("crm.lead") })
            add(buildJsonArray { add("res_id"); add("="); add(leadId) })
        }
        val fields = listOf("id", "res_id", "res_model", "res_name", "activity_type_id", "summary", "note", "date_deadline", "user_id")
        val response = odooService.call(url, JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("mail.activity", "search_read", listOf(domain, JsonArray(fields.map(::JsonPrimitive))))
        )))
        return response.result?.let { json.decodeFromJsonElement<List<MailActivity>>(it) } ?: emptyList()
    }

    suspend fun getMyCalendarEvents(): List<CalendarEvent> {
        val baseUrl = getBaseUrl()
        if (baseUrl.isBlank()) return emptyList()
        
        val userUid = sessionManager.userUid.first() ?: return emptyList()
        val partnerId = sessionManager.partnerId.first()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        
        // Broad domain: owner OR attendee
        // [ '|', ['user_id', '=', uid], ['partner_ids', 'in', [partner_id]] ]
        val domain = mutableListOf<JsonElement>()
        if (partnerId != null) {
            domain.add(JsonPrimitive("|"))
            domain.add(JsonArray(listOf(JsonPrimitive("user_id"), JsonPrimitive("="), JsonPrimitive(userUid))))
            domain.add(JsonArray(listOf(JsonPrimitive("partner_ids"), JsonPrimitive("in"), JsonArray(listOf(JsonPrimitive(partnerId))))))
        } else {
            domain.add(JsonArray(listOf(JsonPrimitive("user_id"), JsonPrimitive("="), JsonPrimitive(userUid))))
        }

        val fields = listOf("id", "name", "start", "stop", "allday", "description", "user_id").map { JsonPrimitive(it) }
        
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("calendar.event", "search_read", listOf(JsonArray(domain), JsonArray(fields)))
        ))
        
        return try {
            val response = odooService.call(url, request)
            response.result?.let { json.decodeFromJsonElement<List<CalendarEvent>>(it) } ?: emptyList()
        } catch (e: Exception) { throw e }
    }

    suspend fun getActivityTypes(): List<MailActivityType> {
        val baseUrl = getBaseUrl()
        if (baseUrl.isBlank()) return emptyList()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        
        val fields = listOf("id", "name", "summary", "icon", "category").map { JsonPrimitive(it) }
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("mail.activity.type", "search_read", listOf(JsonArray(emptyList()), JsonArray(fields)))
        ))
        
        return try {
            val response = odooService.call(url, request)
            response.result?.let { json.decodeFromJsonElement<List<MailActivityType>>(it) } ?: emptyList()
        } catch (e: Exception) { throw e }
    }

    suspend fun scheduleActivity(
        resModel: String,
        resId: Int,
        typeId: Int,
        summary: String,
        note: String,
        dateDeadline: String,
        assignedUserId: Int
    ): Int? {
        val baseUrl = getBaseUrl()
        if (baseUrl.isBlank()) return null
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        
        // Odoo requires a list of dicts for create
        val modelDomain = buildJsonArray { add(buildJsonArray { add("model"); add("="); add(resModel) }) }
        val modelResponse = odooService.call(url, JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("ir.model", "search", listOf(modelDomain), mapOf("limit" to JsonPrimitive(1)))
        )))
        val modelId = modelResponse.result?.jsonArray?.firstOrNull()?.jsonPrimitive?.intOrNull
            ?: error("تعذر تحديد نموذج النشاط؛ تحقق من الصلاحيات")
        val args = listOf(
            buildJsonObject {
                put("res_model_id", modelId)
                put("res_id", resId)
                put("activity_type_id", typeId)
                put("summary", summary)
                put("note", note)
                put("date_deadline", dateDeadline)
                put("user_id", assignedUserId)
            }
        )
        
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("mail.activity", "create", args)
        ))
        
        return try {
            val response = odooService.call(url, request)
            if (response.error != null) {
                android.util.Log.e("OdooRPC", "Activity Error: ${response.error.message}")
                null
            } else {
                response.result?.jsonPrimitive?.intOrNull
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun createCalendarEvent(name: String, start: String, stop: String, description: String, patientId: Int?): Int? {
        val baseUrl = getBaseUrl()
        if (baseUrl.isBlank()) return null
        
        val userUid = sessionManager.userUid.first() ?: return null
        val partnerId = sessionManager.partnerId.first()
        val url = "${baseUrl.trimEnd('/')}/web/dataset/call_kw"
        
        val attendees = mutableListOf<Int>()
        if (partnerId != null) attendees.add(partnerId)
        if (patientId != null && patientId !in attendees) attendees.add(patientId)

        val args = listOf(
            buildJsonObject {
                put("name", name)
                put("start", start)
                put("stop", stop)
                put("description", description)
                put("user_id", userUid)
                if (attendees.isNotEmpty()) {
                    put("partner_ids", JsonArray(listOf(
                        JsonArray(listOf(JsonPrimitive(6), JsonPrimitive(0), JsonArray(attendees.map { JsonPrimitive(it) })))
                    )))
                }
            }
        )
        
        val request = JsonRpcRequest(params = json.encodeToJsonElement(
            CallKwParams("calendar.event", "create", args)
        ))
        
        return try {
            val response = odooService.call(url, request)
            response.result?.let { json.decodeFromJsonElement<Int>(it) }
        } catch (e: Exception) { throw e }
    }
}
