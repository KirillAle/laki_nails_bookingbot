package kirillale.lakinais.db.service

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
 * Слоты генерируются в коде: от time_start до time_end шагом 15 мин, минус перерыв и блокировки.
 */
class SlotAvailabilityService(
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val masterTimeBlockRepository: MasterTimeBlockRepository = MasterTimeBlockRepository(),
    private val procedureRepository: ProcedureRepository = ProcedureRepository(),
) {
    private val slotStepMinutes = 15

    private fun atDayAndTime(day: Instant, timeOfDay: Instant, zoneId: ZoneId): Instant {
        val localDate = day.atZone(zoneId).toLocalDate()
        val localTime = timeOfDay.atZone(zoneId).toLocalTime()
        return localDate.atTime(localTime).atZone(zoneId).toInstant()
    }

    /**
     * Доступные слоты на один день (одну запись master_schedule).
     * Учитываются: перерыв дня, блокировки мастера, занятые бронирования (с полной длительностью).
     */
    fun getAvailableSlotsForDay(
        schedule: MasterScheduleEntity,
        slotDurationSlots: Int = 1,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<AvailableSlot> {
        val scheduleId = schedule.id
        val masterId = schedule.masterId
        val dayDate = schedule.date

        val dayStart = atDayAndTime(dayDate, schedule.timeStart, zoneId)
        val dayEnd = atDayAndTime(dayDate, schedule.timeEnd, zoneId)
        val breakStart = atDayAndTime(dayDate, schedule.breakStart, zoneId)
        val breakEnd = atDayAndTime(dayDate, schedule.breakEnd, zoneId)
        val durationMinutes = slotDurationSlots * slotStepMinutes

        val blocks = masterTimeBlockRepository.findByMasterIdAndDate(masterId, schedule.date)
        val bookedRanges = bookedTimeRanges(scheduleId)

        val slots = mutableListOf<AvailableSlot>()
        var slotStart = dayStart

        while (slotStart.plusSeconds(durationMinutes * 60L) <= dayEnd) {
            val slotEnd = slotStart.plusSeconds(durationMinutes * 60L)
            val inBreak = slotStart < breakEnd && slotEnd > breakStart
            val inBlock = blocks.any { block ->
                slotStart < block.endTime && slotEnd > block.startTime
            }
            val overlapsBooking = bookedRanges.any { (bookedStart, bookedEnd) ->
                slotStart < bookedEnd && slotEnd > bookedStart
            }
            if (!inBreak && !inBlock && !overlapsBooking) {
                slots.add(
                    AvailableSlot(
                        scheduleId = scheduleId,
                        startTime = slotStart,
                        endTime = slotEnd,
                        durationMinutes = durationMinutes,
                    )
                )
            }
            slotStart = slotStart.plusSeconds(slotStepMinutes * 60L)
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

    private fun bookedTimeRanges(scheduleId: UUID): List<Pair<Instant, Instant>> {
        return bookingRepository.findByScheduleId(scheduleId).mapNotNull { booking ->
            val start = booking.startTime ?: return@mapNotNull null
            if (booking.statusName == "CANCELLED") return@mapNotNull null
            val procedure = procedureRepository.findById(booking.procedureId) ?: return@mapNotNull null
            val durationSeconds = procedure.durationSlot * slotStepMinutes * 60L
            start to start.plusSeconds(durationSeconds)
        }
    }
}
