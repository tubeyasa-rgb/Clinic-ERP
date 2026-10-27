package com.minecorerp.clinicerp.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CalendarEvent(
    val id: Int,
    val name: String,
    @SerialName("start") val start: String,
    @SerialName("stop") val stop: String,
    val allday: Boolean = false,
    @Serializable(with = OdooTextSerializer::class) val description: String? = null,
    @SerialName("partner_ids") val partnerIds: List<Int> = emptyList(),
    @SerialName("user_id") val userId: JsonElement? = null
)

@Serializable
data class MailActivity(
    val id: Int,
    @SerialName("res_id") val resId: Int,
    @SerialName("res_model") val resModel: String,
    @SerialName("res_name") val resName: String? = null,
    @SerialName("activity_type_id") val activityTypeId: JsonElement? = null, // [id, name]
    @Serializable(with = OdooTextSerializer::class) val summary: String? = null,
    @Serializable(with = OdooTextSerializer::class) val note: String? = null,
    @SerialName("date_deadline") val dateDeadline: String,
    @SerialName("user_id") val userId: JsonElement? = null,
    @SerialName("activity_category") val category: String? = null
)

@Serializable
data class MailActivityType(
    val id: Int,
    val name: String,
    @Serializable(with = OdooTextSerializer::class) val summary: String? = null,
    @Serializable(with = OdooTextSerializer::class) val icon: String? = null,
    @Serializable(with = OdooTextSerializer::class) val category: String? = null
)
