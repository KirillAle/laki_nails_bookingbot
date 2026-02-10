package kirillale.lakinais.db.service

import kirillale.lakinais.db.entities.TimeSlotEntity
import kirillale.lakinais.db.repositiries.TimeSlotRepository
import java.util.UUID

class TimeSlotService(
    private val timeSlotRepository: TimeSlotRepository = TimeSlotRepository()
) {

    fun createTimeSlot(minutes: Int): TimeSlotEntity {
        return timeSlotRepository.createTimeSlot(minutes)
    }

    fun getById(id: UUID): TimeSlotEntity? {
        return timeSlotRepository.findById(id)
    }

    fun getByMinutes(minutes: Int): TimeSlotEntity? {
        return timeSlotRepository.findByMinutes(minutes)
    }

    fun getAllTimeSlots(): List<TimeSlotEntity> {
        return timeSlotRepository.findAll()
    }

    /**
     * Получить стандартный таймслот (15 минут) или создать его, если не существует
     */
    fun getStandardTimeSlot(): TimeSlotEntity {
        return timeSlotRepository.findByMinutes(15) 
            ?: timeSlotRepository.createTimeSlot(15)
    }
}
