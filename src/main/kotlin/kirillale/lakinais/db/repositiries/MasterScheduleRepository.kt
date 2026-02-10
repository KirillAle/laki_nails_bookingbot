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
        }

        db.sequenceOf(MasterScheduleTable).add(entity)
        return entity
    }
}
