package kirillale.lakinais.db.repositiries

import kirillale.lakinais.db.DatabaseFactory
import kirillale.lakinais.db.entities.MasterTimeBlockEntity
import kirillale.lakinais.db.tables.MasterTimeBlockTable
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.*
import java.time.Instant
import java.util.UUID

class MasterTimeBlockRepository {

    private val db = DatabaseFactory.db

    fun findById(id: UUID): MasterTimeBlockEntity? {
        return db.sequenceOf(MasterTimeBlockTable)
            .firstOrNull { it.id eq id }
    }

    fun findByMasterId(masterId: UUID): List<MasterTimeBlockEntity> {
        return db.sequenceOf(MasterTimeBlockTable)
            .filter { it.master_id eq masterId }
            .toList()
    }

    fun findByMasterIdAndDate(masterId: UUID, date: Instant): List<MasterTimeBlockEntity> {
        return db.sequenceOf(MasterTimeBlockTable)
            .filter { 
                (it.master_id eq masterId) and (it.date eq date)
            }
            .toList()
    }

    fun createTimeBlock(
        masterId: UUID,
        date: Instant,
        startTime: Instant,
        endTime: Instant,
        reason: String? = null
    ): MasterTimeBlockEntity {
        val entity = MasterTimeBlockEntity {
            id = UUID.randomUUID()
            this.masterId = masterId
            this.date = date
            this.startTime = startTime
            this.endTime = endTime
            this.reason = reason
            createdAt = Instant.now()
        }

        db.sequenceOf(MasterTimeBlockTable).add(entity)
        return entity
    }

    fun deleteById(id: UUID): Boolean {
        val entity = findById(id) ?: return false
        entity.delete()
        return true
    }
}
