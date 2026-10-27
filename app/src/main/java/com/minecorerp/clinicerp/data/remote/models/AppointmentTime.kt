package com.minecorerp.clinicerp.data.remote.models

import java.time.*
import java.time.format.DateTimeFormatter

object AppointmentTime {
    private val format = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    fun utcRange(date: String, time: String, minutes: String, zone: ZoneId = ZoneId.systemDefault()): Pair<String, String> {
        val duration = minutes.trim().toLongOrNull()
        require(duration != null && duration in 1..1440) { "المدة لازم تكون من 1 إلى 1440 دقيقة" }
        val local = LocalDateTime.of(LocalDate.parse(date.trim()), LocalTime.parse(time.trim()))
        require(zone.rules.getValidOffsets(local).isNotEmpty()) { "هذا الوقت غير موجود بسبب تغيير التوقيت الصيفي" }
        val start = local.atZone(zone)
        return start.withZoneSameInstant(ZoneOffset.UTC).format(format) to
            start.plusMinutes(duration).withZoneSameInstant(ZoneOffset.UTC).format(format)
    }
}
