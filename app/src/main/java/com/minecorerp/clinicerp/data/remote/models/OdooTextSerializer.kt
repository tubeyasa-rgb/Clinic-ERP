package com.minecorerp.clinicerp.data.remote.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

object OdooTextSerializer : KSerializer<String?> {
    override val descriptor = PrimitiveSerialDescriptor("OdooText", PrimitiveKind.STRING).nullable
    override fun deserialize(decoder: Decoder): String? {
        val value = (decoder as JsonDecoder).decodeJsonElement()
        return (value as? JsonPrimitive)?.takeIf { it.isString }?.content
    }
    override fun serialize(encoder: Encoder, value: String?) {
        (encoder as JsonEncoder).encodeJsonElement(value?.let(::JsonPrimitive) ?: JsonNull)
    }
}
