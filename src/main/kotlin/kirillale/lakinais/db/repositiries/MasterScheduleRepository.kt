package kirillale.lakinais.db.repositiries

import kirillale.lakinais.db.DatabaseFactory
import kirillale.lakinais.db.entities.MasterScheduleEntity
import kirillale.lakinais.db.tables.MasterScheduleTable
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.*
import java.time.Instant
import java.util.UUID

class MasterScheduleRepository {

    private val db = DatabaseFactory.db

    fun ensureSchema() {
        db.useConnection { conn ->
            conn.createStatement().execute(
                """
                ALTER TABLE master_schedule
                ADD COLUMN IF NOT EXISTS is_open BOOLEAN NOT NULL DEFAULT TRUE
                """.trimIndent(),
            )
        }
    }

    fun findById(id: UUID): MasterScheduleEntity? {
        return db.sequenceOf(MasterScheduleTable)
            .firstOrNull { it.id eq id }
    }

    fun findByMasterId(masterId: UUID): List<MasterScheduleEntity> {
        return db.sequenceOf(MasterScheduleTable)
            .filter { it.master_id eq masterId }
            .toList()
    }

    fun findByMasterIdAndDate(masterId: UUID, date: Instant): List<MasterScheduleEntity> {
        return db.sequenceOf(MasterScheduleTable)
            .filter {
                (it.master_id eq masterId) and (it.date eq date)
            }
            .toList()
    }

    /** Вариант А: одна строка на (мастер, дата). Возвращает её или null. */
    fun findOneByMasterIdAndDate(masterId: UUID, date: Instant): MasterScheduleEntity? {
        return findByMasterIdAndDate(masterId, date).firstOrNull()
    }

    fun createSchedule(
        masterId: UUID,
        date: Instant,
        timeStart: Instant,
        timeEnd: Instant,
        breakStart: Instant,
        breakEnd: Instant
    ): MasterScheduleEntity {
        val entity = MasterScheduleEntity {
            id = UUID.randomUUID()
            this.masterId = masterId
            this.date = date
            this.timeStart = timeStart
            this.timeEnd = timeEnd
            this.breakStart = breakStart
            this.breakEnd = breakEnd
            isOpen = true
        }

        db.sequenceOf(MasterScheduleTable).add(entity)
        return entity
    }

    fun setOpen(id: UUID, open: Boolean): MasterScheduleEntity? {
        val entity = findById(id) ?: return null
        entity.isOpen = open
        entity.flushChanges()
        return entity
    }

    fun deleteById(id: UUID): Boolean {
        val entity = findById(id) ?: return false
        entity.delete()
        return true
    }

    fun updateDayHours(
        id: UUID,
        timeStart: Instant,
        timeEnd: Instant,
        breakStart: Instant,
        breakEnd: Instant,
    ): MasterScheduleEntity? {
        val entity = findById(id) ?: return null
        entity.timeStart = timeStart
        entity.timeEnd = timeEnd
        entity.breakStart = breakStart
        entity.breakEnd = breakEnd
        entity.flushChanges()
        return entity
    }
}
