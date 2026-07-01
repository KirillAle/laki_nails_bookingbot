package kirillale.lakinais.db.service

import kirillale.lakinais.booking.BookingIntervals
import kirillale.lakinais.booking.SalonTime
import kirillale.lakinais.db.entities.MasterScheduleEntity
import kirillale.lakinais.db.model.AvailableSlot
import kirillale.lakinais.db.repositiries.BookingRepository
import kirillale.lakinais.db.repositiries.MasterTimeBlockRepository
import kirillale.lakinais.db.repositiries.ProcedureRepository
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * Расчёт доступных слотов по варианту А: одна строка master_schedule = один день.
 * Старт записи показывается только на начале часа, минус перерыв и блокировки.
 */
class SlotAvailabilityService(
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val masterTimeBlockRepository: MasterTimeBlockRepository = MasterTimeBlockRepository(),
    private val procedureRepository: ProcedureRepository = ProcedureRepository(),
) {
    private val slotStartStepMinutes = 60

    private fun atDayAndTime(day: Instant, timeOfDay: Instant, zoneId: ZoneId): Instant {
        val localDate = day.atZone(zoneId).toLocalDate()
        val localTime = timeOfDay.atZone(zoneId).toLocalTime()
        return localDate.atTime(localTime).atZone(zoneId).toInstant()
    }

    /**
     * Доступные слоты на один день (одну запись master_schedule).
     * Учитываются: перерыв дня, блокировки мастера, занятые бронирования (полная длительность услуги
     * + 15 мин перерыв только после конца записи). Слот показывается только если вся услуга влезает.
     */
    fun getAvailableSlotsForDay(
        schedule: MasterScheduleEntity,
        slotDurationSlots: Int = 1,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<AvailableSlot> {
        if (!schedule.isOpen) return emptyList()
        val scheduleId = schedule.id
        val masterId = schedule.masterId
        val dayDate = schedule.date

        val dayStart = atDayAndTime(dayDate, schedule.timeStart, zoneId)
        val dayEnd = atDayAndTime(dayDate, schedule.timeEnd, zoneId)
        val hasBreak = kirillale.lakinais.booking.schedule.ScheduleMapper.hasBreak(schedule, zoneId)
        val breakStart = if (hasBreak) atDayAndTime(dayDate, schedule.breakStart, zoneId) else dayStart
        val breakEnd = if (hasBreak) atDayAndTime(dayDate, schedule.breakEnd, zoneId) else dayStart
        val durationMinutes = slotDurationSlots * BookingIntervals.SLOT_MINUTES

        val durationSeconds = durationMinutes * 60L
        val blocks = masterTimeBlockRepository.findByMasterIdAndDate(masterId, schedule.date)
        val bookedRanges = bookedTimeRanges(schedule, zoneId)

        val slots = mutableListOf<AvailableSlot>()
        var slotStart = firstWholeHourAtOrAfter(dayStart, zoneId)

        while (slotStart.plusSeconds(durationSeconds) <= dayEnd) {
            val slotEnd = slotStart.plusSeconds(durationSeconds)
            val inBreak = hasBreak && slotStart < breakEnd && slotEnd > breakStart
            val inBlock = blocks.any { block ->
                slotStart < block.endTime && slotEnd > block.startTime
            }
            val fitsBookings = BookingIntervals.fitsBetweenBookings(
                candidateStart = slotStart,
                durationSlots = slotDurationSlots,
                occupiedRanges = bookedRanges,
            )
            if (!inBreak && !inBlock && fitsBookings) {
                slots.add(
                    AvailableSlot(
                        scheduleId = scheduleId,
                        startTime = slotStart,
                        endTime = slotEnd,
                        durationMinutes = durationMinutes,
                    )
                )
            }
            slotStart = slotStart.plusSeconds(slotStartStepMinutes * 60L)
        }
        return slots
    }

    fun getAvailableSlotsForTwoProcedures(
        schedule: MasterScheduleEntity,
        durationSlots1: Int,
        durationSlots2: Int,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<AvailableSlot> {
        val totalSlots = durationSlots1 + durationSlots2
        return getAvailableSlotsForDay(schedule, slotDurationSlots = totalSlots, zoneId = zoneId)
    }

    private fun bookedTimeRanges(schedule: MasterScheduleEntity, zoneId: ZoneId): List<Pair<Instant, Instant>> {
        return bookingRepository.findByScheduleId(schedule.id).mapNotNull { booking ->
            val storedStart = booking.startTime ?: return@mapNotNull null
            if (booking.statusName == "CANCELLED") return@mapNotNull null
            val procedure = procedureRepository.findById(booking.procedureId) ?: return@mapNotNull null
            val start = SalonTime.bookingStartOnDay(schedule.date, storedStart, zoneId)
            start to BookingIntervals.slotEnd(start, procedure.durationSlot)
        }
    }

    private fun firstWholeHourAtOrAfter(instant: Instant, zoneId: ZoneId): Instant {
        val local = instant.atZone(zoneId)
        val wholeHour = local.withMinute(0).withSecond(0).withNano(0)
        return (if (local == wholeHour) wholeHour else wholeHour.plusHours(1)).toInstant()
    }
}
