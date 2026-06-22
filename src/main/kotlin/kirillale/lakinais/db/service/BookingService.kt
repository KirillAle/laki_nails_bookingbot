package kirillale.lakinais.db.service

import kirillale.lakinais.db.entities.BookingEntity
import kirillale.lakinais.db.repositiries.BookingRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class BookingService(
    private val bookingRepository: BookingRepository = BookingRepository()
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
        startTime: Instant? = null
    ): BookingEntity {
        require(startTime != null) { "Для варианта А (1 день = 1 строка) startTime обязателен" }
        val existing = bookingRepository.findByScheduleIdAndStartTime(scheduleId, startTime)
        if (existing != null && existing.statusName != "CANCELLED") {
            throw IllegalStateException("Слот уже занят: день=$scheduleId, время=$startTime")
        }
        return bookingRepository.createBooking(
            clientId = clientId,
            scheduleId = scheduleId,
            procedureId = procedureId,
            statusName = initialStatus,
            priceSnapshot = priceSnapshot,
            startTime = startTime
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

    /** Свободен ли слот в этот день в это время (вариант А). */
    fun isSlotAvailable(scheduleId: UUID, startTime: Instant): Boolean {
        val existing = bookingRepository.findByScheduleIdAndStartTime(scheduleId, startTime)
        return existing == null || existing.statusName == "CANCELLED"
    }

    /** Все бронирования на день (для мастера / проверок). */
    fun getBookingsByScheduleId(scheduleId: UUID): List<BookingEntity> {
        return bookingRepository.findByScheduleId(scheduleId)
    }

    fun getById(id: UUID): BookingEntity? {
        return bookingRepository.findById(id)
    }
}