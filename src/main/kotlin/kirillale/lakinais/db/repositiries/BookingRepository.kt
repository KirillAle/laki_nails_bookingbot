package kirillale.lakinais.db.repositiries

import kirillale.lakinais.db.DatabaseFactory
import kirillale.lakinais.db.entities.BookingEntity
import kirillale.lakinais.db.tables.BookingTable
import org.ktorm.dsl.eq
import org.ktorm.entity.*
import java.time.Instant
import java.util.UUID

class BookingRepository {

    private val db = DatabaseFactory.db

    fun findById(id: UUID): BookingEntity? {
        return db.sequenceOf(BookingTable)
            .firstOrNull { it.id eq id }
    }

    fun findByClientId(clientId: UUID): List<BookingEntity> {
        return db.sequenceOf(BookingTable)
            .filter { it.client_id eq clientId }
            .toList()
    }

    fun findByMasterId(masterId: UUID): List<BookingEntity> {
        return db.sequenceOf(BookingTable)
            .filter { it.master_id eq masterId }
            .toList()
    }

    fun findByScheduleId(scheduleId: UUID): BookingEntity? {
        return db.sequenceOf(BookingTable)
            .firstOrNull { it.schedule_id eq scheduleId }
    }

    fun findByStatus(statusName: String): List<BookingEntity> {
        return db.sequenceOf(BookingTable)
            .filter { it.status eq statusName }
            .toList()
    }

    fun createBooking(
        clientId: UUID,
        masterId: UUID,
        scheduleId: UUID,
        procedureId: UUID,
        statusName: String
    ): BookingEntity {
        val entity = BookingEntity {
            id = UUID.randomUUID()
            this.clientId = clientId
            this.masterId = masterId
            this.scheduleId = scheduleId
            this.procedureId = procedureId
            this.statusName = statusName
            createdAt = Instant.now()
        }

        db.sequenceOf(BookingTable).add(entity)
        return entity
    }

    fun updateStatus(id: UUID, newStatus: String): BookingEntity? {
        val existing = findById(id) ?: return null
        existing.statusName = newStatus
        existing.flushChanges()
        return existing
    }
}