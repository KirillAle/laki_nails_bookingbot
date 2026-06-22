package kirillale.lakinais.db.repositiries

import kirillale.lakinais.db.DatabaseFactory
import kirillale.lakinais.db.entities.BookingEntity
import kirillale.lakinais.db.tables.BookingTable
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.*
import java.math.BigDecimal
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

    /** Все бронирования на этот день (вариант А: schedule_id = день). */
    fun findByScheduleId(scheduleId: UUID): List<BookingEntity> {
        return db.sequenceOf(BookingTable)
            .filter { it.schedule_id eq scheduleId }
            .toList()
    }

    /** Занят ли конкретный слот в этот день в это время (вариант А). */
    fun findByScheduleIdAndStartTime(scheduleId: UUID, startTime: Instant): BookingEntity? {
        return db.sequenceOf(BookingTable)
            .firstOrNull {
                (it.schedule_id eq scheduleId) and (it.start_time eq startTime)
            }
    }

    fun findByStatus(statusName: String): List<BookingEntity> {
        return db.sequenceOf(BookingTable)
            .filter { it.status eq statusName }
            .toList()
    }

    fun createBooking(
        clientId: UUID,
        scheduleId: UUID,
        procedureId: UUID,
        statusName: String,
        priceSnapshot: BigDecimal? = null,
        startTime: Instant? = null
    ): BookingEntity {
        val entity = BookingEntity {
            id = UUID.randomUUID()
            this.clientId = clientId
            this.scheduleId = scheduleId
            this.procedureId = procedureId
            this.statusName = statusName
            this.priceSnapshot = priceSnapshot
            this.startTime = startTime
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

    fun updateSlot(id: UUID, scheduleId: UUID, startTime: Instant): BookingEntity? {
        val existing = findById(id) ?: return null
        existing.scheduleId = scheduleId
        existing.startTime = startTime
        existing.flushChanges()
        return existing
    }

    fun findActiveByScheduleId(scheduleId: UUID): List<BookingEntity> =
        findByScheduleId(scheduleId).filter { it.statusName != "CANCELLED" }
}