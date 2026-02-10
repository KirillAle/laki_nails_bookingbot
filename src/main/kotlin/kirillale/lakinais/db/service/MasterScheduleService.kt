package kirillale.lakinais.db.service

import kirillale.lakinais.db.entities.MasterScheduleEntity
import kirillale.lakinais.db.repositiries.MasterScheduleRepository
import java.time.Instant
import java.util.UUID

class MasterScheduleService(
    private val masterScheduleRepository: MasterScheduleRepository = MasterScheduleRepository()
) {

    fun createSchedule(
        masterId: UUID,
        date: Instant,
        timeStart: Instant,
        timeEnd: Instant,
        breakStart: Instant,
        breakEnd: Instant
    ): MasterScheduleEntity {
        return masterScheduleRepository.createSchedule(
            masterId = masterId,
            date = date,
            timeStart = timeStart,
            timeEnd = timeEnd,
            breakStart = breakStart,
            breakEnd = breakEnd
        )
    }

    fun getById(id: UUID): MasterScheduleEntity? {
        return masterScheduleRepository.findById(id)
    }

    fun getByMasterId(masterId: UUID): List<MasterScheduleEntity> {
        return masterScheduleRepository.findByMasterId(masterId)
    }

    fun getByMasterIdAndDate(masterId: UUID, date: Instant): List<MasterScheduleEntity> {
        return masterScheduleRepository.findByMasterIdAndDate(masterId, date)
    }
}
