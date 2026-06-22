package kirillale.lakinais.db.service

import kirillale.lakinais.booking.AvailableDate
import kirillale.lakinais.booking.BookingSlotMode
import kirillale.lakinais.booking.DateInterval
import kirillale.lakinais.booking.DateIntervalBuilder
import kirillale.lakinais.booking.IntervalSearchResult
import kirillale.lakinais.bot.BotProcedureOption
import kirillale.lakinais.db.entities.MasterScheduleEntity
import kirillale.lakinais.db.model.AvailableSlot
import kirillale.lakinais.db.repositiries.MasterScheduleRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class BookingAvailabilityService(
    private val masterScheduleRepository: MasterScheduleRepository = MasterScheduleRepository(),
    private val slotAvailabilityService: SlotAvailabilityService = SlotAvailabilityService(),
) {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun searchIntervals(
        masterId: UUID,
        procedures: List<BotProcedureOption>,
        mode: BookingSlotMode,
        splitPriority: BotProcedureOption?,
        zoneId: ZoneId,
    ): IntervalSearchResult {
        require(procedures.isNotEmpty()) { "Нужна хотя бы одна процедура" }
        val today = LocalDate.now(zoneId)
        val intervals = DateIntervalBuilder.visibleIntervals(today)
        val schedules = openSchedulesInHorizon(masterId, today, zoneId)

        if (schedules.isEmpty()) return IntervalSearchResult.NothingAvailable

        if (procedures.size == 2 && mode == BookingSlotMode.CONSECUTIVE) {
            val manicure = procedures.first { it.procedureType == "Маникюр" }
            val pedicure = procedures.first { it.procedureType == "Педикюр" }
            val hasAnyConsecutive = schedules.any { schedule ->
                dayInHorizon(schedule, today, zoneId) &&
                    slotAvailabilityService.getAvailableSlotsForTwoProcedures(
                        schedule, manicure.durationSlots, pedicure.durationSlots, zoneId
                    ).isNotEmpty()
            }
            if (!hasAnyConsecutive) return IntervalSearchResult.ConsecutiveUnavailable
        }

        val available = intervals.filter { interval ->
            hasAvailabilityInInterval(
                schedules, interval, procedures, mode, splitPriority, today, zoneId
            )
        }
        return if (available.isEmpty()) {
            IntervalSearchResult.NothingAvailable
        } else {
            IntervalSearchResult.Intervals(available)
        }
    }

    fun getAvailableDatesInInterval(
        masterId: UUID,
        interval: DateInterval,
        procedures: List<BotProcedureOption>,
        mode: BookingSlotMode,
        splitPriority: BotProcedureOption?,
        zoneId: ZoneId,
    ): List<AvailableDate> {
        val today = LocalDate.now(zoneId)
        val schedules = openSchedulesInHorizon(masterId, today, zoneId)
            .filter { schedule ->
                val day = schedule.date.atZone(zoneId).toLocalDate()
                !day.isBefore(interval.from) && !day.isAfter(interval.to)
            }

        return schedules.mapNotNull { schedule ->
            val day = schedule.date.atZone(zoneId).toLocalDate()
            val slots = slotsForSelection(schedule, procedures, mode, splitPriority, zoneId)
            if (slots.isEmpty()) null
            else AvailableDate(
                scheduleId = schedule.id,
                date = day,
                label = DateIntervalBuilder.formatDayLabel(day),
            )
        }.sortedBy { it.date }
    }

    fun getAvailableTimeSlots(
        scheduleId: UUID,
        procedures: List<BotProcedureOption>,
        mode: BookingSlotMode,
        splitPriority: BotProcedureOption?,
        zoneId: ZoneId,
    ): List<AvailableSlot> {
        val schedule = masterScheduleRepository.findById(scheduleId) ?: return emptyList()
        return slotsForSelection(schedule, procedures, mode, splitPriority, zoneId)
    }

    fun formatSlotTime(slot: AvailableSlot, zoneId: ZoneId): String {
        val start = LocalTime.from(slot.startTime.atZone(zoneId))
        val end = LocalTime.from(slot.endTime.atZone(zoneId))
        return "${timeFormatter.format(start)}–${timeFormatter.format(end)}"
    }

    private fun slotsForSelection(
        schedule: MasterScheduleEntity,
        procedures: List<BotProcedureOption>,
        mode: BookingSlotMode,
        splitPriority: BotProcedureOption?,
        zoneId: ZoneId,
    ): List<AvailableSlot> {
        val slots = when {
            procedures.size == 1 -> {
                slotAvailabilityService.getAvailableSlotsForDay(
                    schedule, procedures.first().durationSlots, zoneId,
                )
            }
            mode == BookingSlotMode.SPLIT -> {
                val active = splitPriority ?: procedures.first()
                slotAvailabilityService.getAvailableSlotsForDay(schedule, active.durationSlots, zoneId)
            }
            else -> {
                val manicure = procedures.first { it.procedureType == "Маникюр" }
                val pedicure = procedures.first { it.procedureType == "Педикюр" }
                slotAvailabilityService.getAvailableSlotsForTwoProcedures(
                    schedule, manicure.durationSlots, pedicure.durationSlots, zoneId,
                )
            }
        }
        return filterPastSlots(slots, schedule, zoneId)
    }

    private fun filterPastSlots(
        slots: List<AvailableSlot>,
        schedule: MasterScheduleEntity,
        zoneId: ZoneId,
    ): List<AvailableSlot> {
        val scheduleDay = schedule.date.atZone(zoneId).toLocalDate()
        val today = LocalDate.now(zoneId)
        if (scheduleDay.isAfter(today)) return slots
        if (scheduleDay.isBefore(today)) return emptyList()
        val now = java.time.Instant.now()
        return slots.filter { it.startTime.isAfter(now) }
    }

    private fun hasAvailabilityInInterval(
        schedules: List<MasterScheduleEntity>,
        interval: DateInterval,
        procedures: List<BotProcedureOption>,
        mode: BookingSlotMode,
        splitPriority: BotProcedureOption?,
        today: LocalDate,
        zoneId: ZoneId,
    ): Boolean = schedules.any { schedule ->
        val day = schedule.date.atZone(zoneId).toLocalDate()
        dayInHorizon(schedule, today, zoneId) &&
            !day.isBefore(interval.from) &&
            !day.isAfter(interval.to) &&
            slotsForSelection(schedule, procedures, mode, splitPriority, zoneId).isNotEmpty()
    }

    private fun openSchedulesInHorizon(
        masterId: UUID,
        today: LocalDate,
        zoneId: ZoneId,
    ): List<MasterScheduleEntity> {
        val horizonEnd = today.plusDays(DateIntervalBuilder.BOOKING_HORIZON_DAYS)
        return masterScheduleRepository.findByMasterId(masterId)
            .filter { schedule ->
                val day = schedule.date.atZone(zoneId).toLocalDate()
                !day.isBefore(today) && !day.isAfter(horizonEnd)
            }
            .sortedBy { it.date }
    }

    private fun dayInHorizon(
        schedule: MasterScheduleEntity,
        today: LocalDate,
        zoneId: ZoneId,
    ): Boolean {
        val day = schedule.date.atZone(zoneId).toLocalDate()
        val horizonEnd = today.plusDays(DateIntervalBuilder.BOOKING_HORIZON_DAYS)
        return !day.isBefore(today) && !day.isAfter(horizonEnd)
    }
}
