package com.minecorerp.clinicerp.data.remote.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

fun JsonElement?.asOdooId(): Int? {
    return when (this) {
        is JsonArray -> (get(0) as? JsonPrimitive)?.content?.toIntOrNull()
        is JsonPrimitive -> content.toIntOrNull()
        else -> null
    }
}

fun JsonElement?.asOdooName(): String? {
    return when (this) {
        is JsonArray -> (get(1) as? JsonPrimitive)?.content
        is JsonPrimitive -> if (isString) content else null
        else -> null
    }
}

fun JsonElement?.asOdooString(): String? {
    return if (this is JsonPrimitive && isString) content else null
}

fun JsonElement?.asOdooDouble(): Double? {
    return if (this is JsonPrimitive) content.toDoubleOrNull() else null
}

@Serializable
data class AuthenticateParams(
    val db: String,
    val login: String,
    val password: String
)

@Serializable
data class OdooSession(
    val uid: Long,
    val name: String,
    val db: String,
    @SerialName("sid") val sessionId: String? = null
)

@Serializable
data class CallKwParams(
    val model: String,
    val method: String,
    val args: List<JsonElement>,
    val kwargs: Map<String, JsonElement> = emptyMap()
)

interface OdooNamedModel {
    val id: Int
    val name: String
}

@Serializable
data class CrmStage(
    override val id: Int,
    override val name: String,
    val sequence: Int
) : OdooNamedModel

@Serializable
data class CrmTag(
    override val id: Int,
    override val name: String,
    val color: Int? = null
) : OdooNamedModel

@Serializable
data class ResUser(
    override val id: Int,
    override val name: String,
    @Serializable(with = OdooTextSerializer::class) val email: String? = null,
    @Serializable(with = OdooTextSerializer::class) val phone: String? = null
) : OdooNamedModel

@Serializable
data class CrmTeam(
    override val id: Int,
    override val name: String
) : OdooNamedModel

@Serializable
data class UtmSource(
    override val id: Int,
    override val name: String
) : OdooNamedModel

@Serializable
data class UtmMedium(
    override val id: Int,
    override val name: String
) : OdooNamedModel

@Serializable
data class UtmCampaign(
    override val id: Int,
    override val name: String
) : OdooNamedModel

@Serializable
data class CrmLead(
    val id: Int,
    val name: String,
    @SerialName("contact_name") val contactName: JsonElement? = null,
    val phone: JsonElement? = null,
    @SerialName("email_from") val emailFrom: JsonElement? = null,
    @SerialName("stage_id") val stageId: JsonElement? = null,
    @SerialName("partner_id") val partnerId: JsonElement? = null, 
    @SerialName("expected_revenue") val expectedRevenue: JsonElement? = null,
    @SerialName("create_date") val createDate: JsonElement? = null,
    val description: JsonElement? = null,
    @SerialName("date_deadline") val dateDeadline: JsonElement? = null,
    @SerialName("user_id") val userId: JsonElement? = null, 
    @SerialName("tag_ids") val tagIds: List<Int>? = null,
    val priority: JsonElement? = null,
    @SerialName("team_id") val teamId: JsonElement? = null,
    val probability: Double? = null,
    @SerialName("campaign_id") val campaignId: JsonElement? = null,
    @SerialName("medium_id") val mediumId: JsonElement? = null,
    @SerialName("source_id") val sourceId: JsonElement? = null
)

@Serializable
data class OdooAttachment(
    val id: Int,
    val name: String,
    @SerialName("mimetype") val mimeType: String? = null,
    @SerialName("datas") val datas: String? = null, // Base64 content
    @SerialName("create_date") val createDate: String? = null,
    @SerialName("file_size") val fileSize: Int? = null
)
