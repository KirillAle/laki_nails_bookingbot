package kirillale.lakinais.booking

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object SalonTime {
    fun dayInstant(date: LocalDate, zoneId: ZoneId): Instant =
        date.atStartOfDay(zoneId).toInstant()

    fun atTime(date: LocalDate, time: LocalTime, zoneId: ZoneId): Instant =
        date.atTime(time).atZone(zoneId).toInstant()

    fun toLocalDate(instant: Instant, zoneId: ZoneId): LocalDate =
        instant.atZone(zoneId).toLocalDate()

    fun toLocalTime(instant: Instant, zoneId: ZoneId): LocalTime =
        instant.atZone(zoneId).toLocalTime()
}
