package kirillale.lakinais.db.service

import kirillale.lakinais.booking.DateIntervalBuilder
import kirillale.lakinais.booking.SalonTime
import kirillale.lakinais.booking.WorkDayDefaults
import kirillale.lakinais.db.entities.BookingEntity
import kirillale.lakinais.db.entities.MasterScheduleEntity
import kirillale.lakinais.db.repositiries.AccountRepository
import kirillale.lakinais.db.repositiries.BookingRepository
import kirillale.lakinais.db.repositiries.MasterScheduleRepository
import kirillale.lakinais.db.repositiries.ProcedureRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

data class OpenPeriodResult(
    val created: Int,
    val skipped: Int,
    val from: LocalDate,
    val to: LocalDate,
)

data class BookingView(
    val bookingId: UUID,
    val clientName: String,
    val clientPhone: String?,
    val procedureLabel: String,
    val status: String,
    val startTime: Instant,
    val scheduleId: UUID,
    val dateLabel: String,
    val timeLabel: String,
)

class ScheduleManagementService(
    private val masterScheduleRepository: MasterScheduleRepository = MasterScheduleRepository(),
    private val bookingRepository: BookingRepository = BookingRepository(),
) {
    fun openPeriod(
        masterId: UUID,
        startDate: LocalDate,
        days: Int,
        defaults: WorkDayDefaults,
        zoneId: ZoneId,
    ): OpenPeriodResult {
        require(days in 1..90) { "Количество дней: от 1 до 90" }
        var created = 0
        var skipped = 0
        val endDate = startDate.plusDays(days - 1L)

        for (offset in 0 until days) {
            val date = startDate.plusDays(offset.toLong())
            if (findScheduleForDay(masterId, date, zoneId) != null) {
                skipped++
                continue
            }
            createDay(masterId, date, defaults, zoneId)
            created++
        }
        return OpenPeriodResult(created, skipped, startDate, endDate)
    }

    fun closeDay(masterId: UUID, scheduleId: UUID, zoneId: ZoneId): Result<Unit> {
        val schedule = masterScheduleRepository.findById(scheduleId)
            ?: return Result.failure(IllegalArgumentException("День не найден"))
        if (schedule.masterId != masterId) {
            return Result.failure(IllegalStateException("Нет доступа к этому дню"))
        }
        val active = bookingRepository.findActiveByScheduleId(scheduleId)
        if (active.isNotEmpty()) {
            return Result.failure(IllegalStateException("На этот день есть активные записи (${active.size}). Сначала отмените или перенесите их."))
        }
        masterScheduleRepository.deleteById(scheduleId)
        return Result.success(Unit)
    }

    fun listOpenDays(masterId: UUID, from: LocalDate, to: LocalDate, zoneId: ZoneId): List<MasterScheduleEntity> =
        masterScheduleRepository.findByMasterId(masterId)
            .filter { schedule ->
                val day = SalonTime.toLocalDate(schedule.date, zoneId)
                !day.isBefore(from) && !day.isAfter(to)
            }
            .sortedBy { it.date }

    fun findScheduleForDay(masterId: UUID, date: LocalDate, zoneId: ZoneId): MasterScheduleEntity? =
        masterScheduleRepository.findByMasterId(masterId)
            .firstOrNull { SalonTime.toLocalDate(it.date, zoneId) == date }

    private fun createDay(
        masterId: UUID,
        date: LocalDate,
        defaults: WorkDayDefaults,
        zoneId: ZoneId,
    ): MasterScheduleEntity = masterScheduleRepository.createSchedule(
        masterId = masterId,
        date = SalonTime.dayInstant(date, zoneId),
        timeStart = SalonTime.atTime(date, defaults.workStart, zoneId),
        timeEnd = SalonTime.atTime(date, defaults.workEnd, zoneId),
        breakStart = SalonTime.atTime(date, defaults.breakStart, zoneId),
        breakEnd = SalonTime.atTime(date, defaults.breakEnd, zoneId),
    )
}

class BookingQueryService(
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val accountRepository: AccountRepository = AccountRepository(),
    private val procedureRepository: ProcedureRepository = ProcedureRepository(),
    private val masterScheduleRepository: MasterScheduleRepository = MasterScheduleRepository(),
) {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun listForDay(scheduleId: UUID, zoneId: ZoneId): List<BookingView> {
        val schedule = masterScheduleRepository.findById(scheduleId) ?: return emptyList()
        return bookingRepository.findActiveByScheduleId(scheduleId)
            .sortedBy { it.startTime }
            .mapNotNull { toView(it, schedule, zoneId) }
    }

    fun listForMasterDay(masterId: UUID, date: LocalDate, zoneId: ZoneId): List<BookingView> {
        val schedule = masterScheduleRepository.findByMasterId(masterId)
            .firstOrNull { SalonTime.toLocalDate(it.date, zoneId) == date }
            ?: return emptyList()
        return listForDay(schedule.id, zoneId)
    }

    fun getView(bookingId: UUID, zoneId: ZoneId): BookingView? {
        val booking = bookingRepository.findById(bookingId) ?: return null
        val schedule = masterScheduleRepository.findById(booking.scheduleId) ?: return null
        return toView(booking, schedule, zoneId)
    }

    private fun toView(booking: BookingEntity, schedule: MasterScheduleEntity, zoneId: ZoneId): BookingView? {
        val start = booking.startTime ?: return null
        val client = accountRepository.findById(booking.clientId) ?: return null
        val procedure = procedureRepository.findById(booking.procedureId) ?: return null
        val date = SalonTime.toLocalDate(schedule.date, zoneId)
        val time = SalonTime.toLocalTime(start, zoneId)
        val endEstimate = start.plusSeconds(procedure.durationSlot * 15L * 60L)
        val endTime = SalonTime.toLocalTime(endEstimate, zoneId)
        return BookingView(
            bookingId = booking.id,
            clientName = listOf(client.firstName, client.lastName).filter { it.isNotBlank() }.joinToString(" "),
            clientPhone = client.phone,
            procedureLabel = "${procedure.procedureType} / ${procedure.procedureSubtype}",
            status = booking.statusName,
            startTime = start,
            scheduleId = schedule.id,
            dateLabel = DateIntervalBuilder.formatDayLabel(date),
            timeLabel = "${timeFormatter.format(time)}–${timeFormatter.format(endTime)}",
        )
    }
}

class BookingManagementService(
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val bookingService: BookingService = BookingService(),
) {
    fun cancel(bookingId: UUID): BookingEntity? = bookingService.cancelBooking(bookingId)

    fun confirm(bookingId: UUID): BookingEntity? = bookingService.confirmBooking(bookingId)

    fun reschedule(bookingId: UUID, newScheduleId: UUID, newStartTime: Instant): Result<BookingEntity> {
        val booking = bookingRepository.findById(bookingId)
            ?: return Result.failure(IllegalArgumentException("Запись не найдена"))
        if (booking.statusName == "CANCELLED") {
            return Result.failure(IllegalStateException("Запись уже отменена"))
        }
        val conflict = bookingRepository.findByScheduleIdAndStartTime(newScheduleId, newStartTime)
        if (conflict != null && conflict.id != bookingId && conflict.statusName != "CANCELLED") {
            return Result.failure(IllegalStateException("Новый слот уже занят"))
        }
        val updated = bookingRepository.updateSlot(bookingId, newScheduleId, newStartTime)
            ?: return Result.failure(IllegalStateException("Не удалось перенести"))
        return Result.success(updated)
    }
}
