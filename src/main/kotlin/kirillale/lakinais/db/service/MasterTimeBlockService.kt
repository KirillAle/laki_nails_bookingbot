package kirillale.lakinais.db.service

import kirillale.lakinais.db.entities.MasterTimeBlockEntity
import kirillale.lakinais.db.repositiries.MasterTimeBlockRepository
import java.time.Instant
import java.util.UUID

class MasterTimeBlockService(
    private val masterTimeBlockRepository: MasterTimeBlockRepository = MasterTimeBlockRepository()
) {

    fun createTimeBlock(
        masterId: UUID,
        date: Instant,
        startTime: Instant,
        endTime: Instant,
        reason: String? = null
    ): MasterTimeBlockEntity {
        return masterTimeBlockRepository.createTimeBlock(
            masterId = masterId,
            date = date,
            startTime = startTime,
            endTime = endTime,
            reason = reason
        )
    }

    fun getById(id: UUID): MasterTimeBlockEntity? {
        return masterTimeBlockRepository.findById(id)
    }

    fun getByMasterId(masterId: UUID): List<MasterTimeBlockEntity> {
        return masterTimeBlockRepository.findByMasterId(masterId)
    }

    fun getByMasterIdAndDate(masterId: UUID, date: Instant): List<MasterTimeBlockEntity> {
        return masterTimeBlockRepository.findByMasterIdAndDate(masterId, date)
    }

    fun deleteById(id: UUID): Boolean {
        return masterTimeBlockRepository.deleteById(id)
    }
}
