package com.minecorerp.clinicerp

import com.minecorerp.clinicerp.data.remote.*
import com.minecorerp.clinicerp.data.remote.models.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class OdooPayloadTest {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    @Test fun optionalContactTextAcceptsOdooFalse() {
        val contact = json.decodeFromString<ResUser>("""{"id":1,"name":"Patient","email":false,"phone":false}""")
        assertNull(contact.email); assertNull(contact.phone)
    }
    @Test fun optionalCalendarDescriptionAcceptsOdooFalse() {
        val event = json.decodeFromString<CalendarEvent>("""{"id":1,"name":"Visit","start":"2026-10-27 09:00:00","stop":"2026-10-27 10:00:00","description":false}""")
        assertNull(event.description)
    }
    @Test fun patientIdentitySurvivesCrmRead() {
        val lead = json.decodeFromString<CrmLead>("""{"id":1,"name":"Visit","partner_id":[42,"Patient"]}""")
        assertEquals(42, lead.partnerId.asOdooId())
    }
    @Test fun rpcErrorsDoNotBecomeEmptySuccesses() = runBlocking {
        val service = object : OdooService {
            override suspend fun rawCall(url: String, request: JsonRpcRequest) = JsonRpcResponse("2.0", request.id, error = JsonRpcError(200, "Odoo Server Error", JsonRpcErrorData("AccessError", message = "Access denied")))
        }
        try {
            service.call("https://example.invalid", JsonRpcRequest(params = buildJsonObject {}))
            fail("Expected RPC failure")
        } catch (e: IllegalStateException) { assertEquals("Access denied", e.message) }
    }
}
