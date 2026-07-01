package kirillale.lakinais.db.service

import kirillale.lakinais.booking.DateIntervalBuilder
import kirillale.lakinais.booking.SalonTime
import kirillale.lakinais.booking.schedule.DayKindFilter
import kirillale.lakinais.booking.schedule.ScheduleMapper
import kirillale.lakinais.booking.schedule.WeekSchedulePlan
import kirillale.lakinais.booking.schedule.WorkDayProfile
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
    val clientUserName: String?,
    val procedureLabel: String,
    val status: String,
    val startTime: Instant,
    val scheduleId: UUID,
    val dateLabel: String,
    val timeLabel: String,
)

data class ApplyProfileResult(
    val updated: Int,
    val skipped: Int,
)

class ScheduleManagementService(
    private val masterScheduleRepository: MasterScheduleRepository = MasterScheduleRepository(),
    private val bookingRepository: BookingRepository = BookingRepository(),
) {
    fun findScheduleForMaster(masterId: UUID, scheduleId: UUID): MasterScheduleEntity? {
        val schedule = masterScheduleRepository.findById(scheduleId) ?: return null
        if (schedule.masterId != masterId) return null
        return schedule
    }

    fun openPeriod(
        masterId: UUID,
        startDate: LocalDate,
        days: Int,
        plan: WeekSchedulePlan,
        zoneId: ZoneId,
    ): OpenPeriodResult {
        require(days in 1..90) { "Количество дней: от 1 до 90" }
        var created = 0
        var skipped = 0
        val endDate = startDate.plusDays(days - 1L)

        for (offset in 0 until days) {
            val date = startDate.plusDays(offset.toLong())
            val existing = findScheduleForDay(masterId, date, zoneId)
            if (existing != null) {
                if (!existing.isOpen) {
                    val instants = ScheduleMapper.toInstants(date, plan.profileFor(date), zoneId)
                    masterScheduleRepository.setOpen(existing.id, open = true)
                    masterScheduleRepository.updateDayHours(
                        existing.id,
                        instants.timeStart,
                        instants.timeEnd,
                        instants.breakStart,
                        instants.breakEnd,
                    )
                    created++
                } else {
                    skipped++
                }
                continue
            }
            createDay(masterId, date, plan.profileFor(date), zoneId)
            created++
        }
        return OpenPeriodResult(created, skipped, startDate, endDate)
    }

    fun updateDayProfile(
        masterId: UUID,
        scheduleId: UUID,
        profile: WorkDayProfile,
        zoneId: ZoneId,
    ): Result<Unit> {
        val schedule = findScheduleForMaster(masterId, scheduleId)
            ?: return Result.failure(IllegalArgumentException("День не найден"))
        val date = SalonTime.toLocalDate(schedule.date, zoneId)
        val instants = ScheduleMapper.toInstants(date, profile, zoneId)
        masterScheduleRepository.updateDayHours(
            scheduleId,
            instants.timeStart,
            instants.timeEnd,
            instants.breakStart,
            instants.breakEnd,
        ) ?: return Result.failure(IllegalStateException("Не удалось обновить день"))
        return Result.success(Unit)
    }

    fun applyProfileToRange(
        masterId: UUID,
        from: LocalDate,
        to: LocalDate,
        filter: DayKindFilter,
        profile: WorkDayProfile,
        zoneId: ZoneId,
    ): ApplyProfileResult {
        var updated = 0
        var skipped = 0
        val schedules = listOpenDays(masterId, from, to, zoneId)
        for (schedule in schedules) {
            val date = SalonTime.toLocalDate(schedule.date, zoneId)
            if (!filter.matches(date)) {
                skipped++
                continue
            }
            val instants = ScheduleMapper.toInstants(date, profile, zoneId)
            masterScheduleRepository.updateDayHours(
                schedule.id,
                instants.timeStart,
                instants.timeEnd,
                instants.breakStart,
                instants.breakEnd,
            )
            updated++
        }
        return ApplyProfileResult(updated, skipped)
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
        masterScheduleRepository.setOpen(scheduleId, open = false)
            ?: return Result.failure(IllegalStateException("Не удалось закрыть день"))
        return Result.success(Unit)
    }

    fun listOpenDays(masterId: UUID, from: LocalDate, to: LocalDate, zoneId: ZoneId): List<MasterScheduleEntity> =
        masterScheduleRepository.findByMasterId(masterId)
            .filter { schedule ->
                schedule.isOpen && run {
                    val day = SalonTime.toLocalDate(schedule.date, zoneId)
                    !day.isBefore(from) && !day.isAfter(to)
                }
            }
            .sortedBy { it.date }

    fun findScheduleForDay(masterId: UUID, date: LocalDate, zoneId: ZoneId): MasterScheduleEntity? =
        masterScheduleRepository.findByMasterId(masterId)
            .firstOrNull { SalonTime.toLocalDate(it.date, zoneId) == date }

    private fun createDay(
        masterId: UUID,
        date: LocalDate,
        profile: WorkDayProfile,
        zoneId: ZoneId,
    ): MasterScheduleEntity {
        val instants = ScheduleMapper.toInstants(date, profile, zoneId)
        return masterScheduleRepository.createSchedule(
            masterId = masterId,
            date = instants.date,
            timeStart = instants.timeStart,
            timeEnd = instants.timeEnd,
            breakStart = instants.breakStart,
            breakEnd = instants.breakEnd,
        )
    }
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

    /** Все активные записи мастера начиная с [fromDate], по возрастанию времени. */
    fun listUpcomingForMaster(
        masterId: UUID,
        zoneId: ZoneId,
        fromDate: LocalDate = LocalDate.now(zoneId),
    ): List<BookingView> =
        masterScheduleRepository.findByMasterId(masterId)
            .filter { schedule ->
                schedule.isOpen && SalonTime.toLocalDate(schedule.date, zoneId) >= fromDate
            }
            .flatMap { schedule ->
                bookingRepository.findActiveByScheduleId(schedule.id)
                    .mapNotNull { toView(it, schedule, zoneId) }
            }
            .sortedBy { it.startTime }

    fun getView(bookingId: UUID, zoneId: ZoneId): BookingView? {
        val booking = bookingRepository.findById(bookingId) ?: return null
        val schedule = masterScheduleRepository.findById(booking.scheduleId) ?: return null
        return toView(booking, schedule, zoneId)
    }

    fun listActiveForClient(clientId: UUID, zoneId: ZoneId): List<BookingView> =
        bookingRepository.findByClientId(clientId)
            .filter { it.statusName == "PENDING" || it.statusName == "CONFIRMED" }
            .mapNotNull { booking ->
                val schedule = masterScheduleRepository.findById(booking.scheduleId) ?: return@mapNotNull null
                toView(booking, schedule, zoneId)
            }
            .sortedBy { it.startTime }

    private fun toView(booking: BookingEntity, schedule: MasterScheduleEntity, zoneId: ZoneId): BookingView? {
        val storedStart = booking.startTime ?: return null
        val resolvedStart = SalonTime.bookingStartOnDay(schedule.date, storedStart, zoneId)
        val client = accountRepository.findById(booking.clientId) ?: return null
        val procedure = procedureRepository.findById(booking.procedureId) ?: return null
        val date = SalonTime.toLocalDate(schedule.date, zoneId)
        val time = SalonTime.toLocalTime(resolvedStart, zoneId)
        val endEstimate = resolvedStart.plusSeconds(procedure.durationSlot * 15L * 60L)
        val endTime = SalonTime.toLocalTime(endEstimate, zoneId)
        return BookingView(
            bookingId = booking.id,
            clientName = listOf(client.firstName, client.lastName).filter { it.isNotBlank() }.joinToString(" "),
            clientPhone = client.phone,
            clientUserName = client.userName.toTelegramUserName(),
            procedureLabel = "${procedure.procedureType} / ${procedure.procedureSubtype}",
            status = booking.statusName,
            startTime = resolvedStart,
            scheduleId = schedule.id,
            dateLabel = DateIntervalBuilder.formatDayLabel(date),
            timeLabel = "${timeFormatter.format(time)}–${timeFormatter.format(endTime)}",
        )
    }

    private fun String?.toTelegramUserName(): String? {
        val normalized = this?.trim()?.removePrefix("@").orEmpty()
        return normalized.takeIf { it.isNotBlank() }?.let { "@$it" }
    }
}

class BookingManagementService(
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val bookingService: BookingService = BookingService(),
    private val procedureRepository: ProcedureRepository = ProcedureRepository(),
) {
    fun cancel(bookingId: UUID): BookingEntity? = bookingService.cancelBooking(bookingId)

    fun cancelForClient(clientId: UUID, bookingId: UUID): Result<Unit> {
        val booking = bookingRepository.findById(bookingId)
            ?: return Result.failure(IllegalArgumentException("Запись не найдена"))
        if (booking.clientId != clientId) {
            return Result.failure(IllegalStateException("Это не ваша запись"))
        }
        if (booking.statusName == "CANCELLED") {
            return Result.failure(IllegalStateException("Запись уже отменена"))
        }
        cancel(bookingId) ?: return Result.failure(IllegalStateException("Не удалось отменить"))
        return Result.success(Unit)
    }

    fun confirm(bookingId: UUID): BookingEntity? = bookingService.confirmBooking(bookingId)

    fun reschedule(bookingId: UUID, newScheduleId: UUID, newStartTime: Instant): Result<BookingEntity> {
        val booking = bookingRepository.findById(bookingId)
            ?: return Result.failure(IllegalArgumentException("Запись не найдена"))
        if (booking.statusName == "CANCELLED") {
            return Result.failure(IllegalStateException("Запись уже отменена"))
        }
        val procedure = procedureRepository.findById(booking.procedureId)
            ?: return Result.failure(IllegalStateException("Процедура записи не найдена"))
        if (!bookingService.isSlotAvailable(
                scheduleId = newScheduleId,
                startTime = newStartTime,
                durationSlots = procedure.durationSlot,
                excludeBookingId = bookingId,
            )) {
            return Result.failure(IllegalStateException("Новый слот пересекается с другой записью"))
        }
        val updated = bookingRepository.updateSlot(bookingId, newScheduleId, newStartTime)
            ?: return Result.failure(IllegalStateException("Не удалось перенести"))
        return Result.success(updated)
    }
}
