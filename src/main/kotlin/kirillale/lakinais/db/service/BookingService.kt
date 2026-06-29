package kirillale.lakinais.db.service

import kirillale.lakinais.booking.BookingIntervals
import kirillale.lakinais.booking.SalonTime
import kirillale.lakinais.db.entities.BookingEntity
import kirillale.lakinais.db.repositiries.BookingRepository
import kirillale.lakinais.db.repositiries.MasterScheduleRepository
import kirillale.lakinais.db.repositiries.ProcedureRepository
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class BookingService(
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val procedureRepository: ProcedureRepository = ProcedureRepository(),
    private val masterScheduleRepository: MasterScheduleRepository = MasterScheduleRepository(),
) {

    /**
     * Создать новое бронирование (вариант А: день = одна строка master_schedule, слот = день + startTime).
     * startTime обязателен.
     */
    fun createBooking(
        clientId: UUID,
        scheduleId: UUID,
        procedureId: UUID,
        initialStatus: String = "PENDING",
        priceSnapshot: BigDecimal? = null,
        startTime: Instant? = null,
        extraOccupied: List<Pair<Instant, Instant>> = emptyList(),
        zoneId: ZoneId = DEFAULT_ZONE,
    ): BookingEntity {
        require(startTime != null) { "Для варианта А (1 день = 1 строка) startTime обязателен" }
        val procedure = procedureRepository.findById(procedureId)
            ?: throw IllegalStateException("Процедура не найдена: $procedureId")
        if (!isSlotAvailable(
                scheduleId = scheduleId,
                startTime = startTime,
                durationSlots = procedure.durationSlot,
                extraOccupied = extraOccupied,
                zoneId = zoneId,
            )) {
            throw IllegalStateException("Слот пересекается с другой записью: день=$scheduleId, время=$startTime")
        }
        return bookingRepository.createBooking(
            clientId = clientId,
            scheduleId = scheduleId,
            procedureId = procedureId,
            statusName = initialStatus,
            priceSnapshot = priceSnapshot,
            startTime = startTime,
        )
    }

    fun confirmBooking(bookingId: UUID): BookingEntity? {
        return bookingRepository.updateStatus(bookingId, "CONFIRMED")
    }

    fun cancelBooking(bookingId: UUID): BookingEntity? {
        return bookingRepository.updateStatus(bookingId, "CANCELLED")
    }

    fun getClientBookings(clientId: UUID): List<BookingEntity> {
        return bookingRepository.findByClientId(clientId)
    }

    /**
     * Свободен ли интервал на этот день: без пересечения с другими записями мастера
     * и с перерывом [BookingIntervals.BREAK_SLOTS_BETWEEN_BOOKINGS] только после конца другой записи.
     */
    fun isSlotAvailable(
        scheduleId: UUID,
        startTime: Instant,
        durationSlots: Int,
        excludeBookingId: UUID? = null,
        extraOccupied: List<Pair<Instant, Instant>> = emptyList(),
        zoneId: ZoneId = DEFAULT_ZONE,
    ): Boolean {
        val schedule = masterScheduleRepository.findById(scheduleId) ?: return false
        if (!schedule.isOpen) return false
        val slotEnd = BookingIntervals.slotEnd(startTime, durationSlots)
        val bookings = bookingRepository.findByScheduleId(scheduleId)
        for (booking in bookings) {
            if (booking.statusName == "CANCELLED") continue
            if (booking.id == excludeBookingId) continue
            val storedStart = booking.startTime ?: continue
            val bookedStart = SalonTime.bookingStartOnDay(schedule.date, storedStart, zoneId)
            val procedure = procedureRepository.findById(booking.procedureId) ?: continue
            val bookedEnd = BookingIntervals.slotEnd(bookedStart, procedure.durationSlot)
            if (BookingIntervals.conflicts(startTime, slotEnd, bookedStart, bookedEnd)) {
                return false
            }
        }
        if (BookingIntervals.conflictsAny(startTime, slotEnd, extraOccupied, breakSlots = 0)) {
            return false
        }
        return true
    }

    /** Все бронирования на день (для мастера / проверок). */
    fun getBookingsByScheduleId(scheduleId: UUID): List<BookingEntity> {
        return bookingRepository.findByScheduleId(scheduleId)
    }

    fun getById(id: UUID): BookingEntity? {
        return bookingRepository.findById(id)
    }

    companion object {
        private val DEFAULT_ZONE: ZoneId = ZoneId.of("Asia/Tbilisi")
    }
}
