package kirillale.lakinais.db.repositiries

import kirillale.lakinais.db.DatabaseFactory
import kirillale.lakinais.db.entities.TimeSlotEntity
import kirillale.lakinais.db.tables.TimeSlotTable
import org.ktorm.dsl.eq
import org.ktorm.entity.*
import java.util.UUID

class TimeSlotRepository {

    private val db = DatabaseFactory.db

    fun findById(id: UUID): TimeSlotEntity? {
        return db.sequenceOf(TimeSlotTable)
            .firstOrNull { it.id eq id }
    }

    fun findByMinutes(minutes: Int): TimeSlotEntity? {
        return db.sequenceOf(TimeSlotTable)
            .firstOrNull { it.minutes eq minutes }
    }

    fun findAll(): List<TimeSlotEntity> {
        return db.sequenceOf(TimeSlotTable).toList()
    }

    fun createTimeSlot(minutes: Int): TimeSlotEntity {
        val entity = TimeSlotEntity {
            id = UUID.randomUUID()
            this.minutes = minutes
        }

        db.sequenceOf(TimeSlotTable).add(entity)
        return entity
    }
}
