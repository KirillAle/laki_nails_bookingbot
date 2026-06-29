package kirillale.lakinais.booking.schedule

import kirillale.lakinais.booking.SalonTime
import kirillale.lakinais.db.entities.MasterScheduleEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Преобразование между доменным профилем дня и полями master_schedule. */
object ScheduleMapper {

    data class DayInstants(
        val date: Instant,
        val timeStart: Instant,
        val timeEnd: Instant,
        val breakStart: Instant,
        val breakEnd: Instant,
    )

    fun toInstants(date: LocalDate, profile: WorkDayProfile, zoneId: ZoneId): DayInstants {
        val dayInstant = SalonTime.dayInstant(date, zoneId)
        val workStart = SalonTime.atTime(date, profile.workStart, zoneId)
        val workEnd = SalonTime.atTime(date, profile.workEnd, zoneId)
        val (breakStart, breakEnd) = if (profile.breakInterval != null) {
            val br = profile.breakInterval
            SalonTime.atTime(date, br.start, zoneId) to SalonTime.atTime(date, br.end, zoneId)
        } else {
            workStart to workStart
        }
        return DayInstants(dayInstant, workStart, workEnd, breakStart, breakEnd)
    }

    fun fromEntity(schedule: MasterScheduleEntity, zoneId: ZoneId): WorkDayProfile {
        val date = SalonTime.toLocalDate(schedule.date, zoneId)
        val workStart = SalonTime.toLocalTime(schedule.timeStart, zoneId)
        val workEnd = SalonTime.toLocalTime(schedule.timeEnd, zoneId)
        val breakStart = SalonTime.toLocalTime(schedule.breakStart, zoneId)
        val breakEnd = SalonTime.toLocalTime(schedule.breakEnd, zoneId)
        val breakInterval = if (hasBreak(schedule.breakStart, schedule.breakEnd, zoneId)) {
            TimeRange(breakStart, breakEnd)
        } else {
            null
        }
        return WorkDayProfile(workStart, workEnd, breakInterval)
    }

    fun hasBreak(breakStart: Instant, breakEnd: Instant, zoneId: ZoneId): Boolean {
        val start = breakStart.atZone(zoneId).toLocalTime()
        val end = breakEnd.atZone(zoneId).toLocalTime()
        return end > start
    }

    fun hasBreak(schedule: MasterScheduleEntity, zoneId: ZoneId): Boolean =
        hasBreak(schedule.breakStart, schedule.breakEnd, zoneId)
}
