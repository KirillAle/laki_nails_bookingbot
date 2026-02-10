package kirillale.lakinais.db.service

import kirillale.lakinais.db.entities.BookingEntity
import kirillale.lakinais.db.repositiries.BookingRepository
import java.util.UUID

class BookingService(
    private val bookingRepository: BookingRepository = BookingRepository()
) {

    /**
     * Создать новое бронирование с базовой валидацией:
     * - проверяем, что слот (scheduleId) ещё не занят
     */
    fun createBooking(
        clientId: UUID,
        masterId: UUID,
        scheduleId: UUID,
        procedureId: UUID,
        initialStatus: String = "PENDING"
    ): BookingEntity {
        val existingForSlot = bookingRepository.findByScheduleId(scheduleId)
        if (existingForSlot != null) {
            throw IllegalStateException("Слот уже занят для scheduleId=$scheduleId")
        }

        return bookingRepository.createBooking(
            clientId = clientId,
            masterId = masterId,
            scheduleId = scheduleId,
            procedureId = procedureId,
            statusName = initialStatus
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

    fun getMasterBookings(masterId: UUID): List<BookingEntity> {
        return bookingRepository.findByMasterId(masterId)
    }

    fun isSlotAvailable(scheduleId: UUID): Boolean {
        return bookingRepository.findByScheduleId(scheduleId) == null
    }

    fun getById(id: UUID): BookingEntity? {
        return bookingRepository.findById(id)
    }
}