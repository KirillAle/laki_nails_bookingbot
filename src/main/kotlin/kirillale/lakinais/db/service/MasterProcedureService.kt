package kirillale.lakinais.db.service

import kirillale.lakinais.db.entities.MasterProcedureEntity
import kirillale.lakinais.db.repositiries.MasterProcedureRepository
import java.util.UUID

class MasterProcedureService(
    private val masterProcedureRepository: MasterProcedureRepository = MasterProcedureRepository()
) {

    fun createMasterProcedure(
        masterId: UUID,
        procedureId: UUID
    ): MasterProcedureEntity {
        return masterProcedureRepository.createMasterProcedure(
            masterId = masterId,
            procedureId = procedureId
        )
    }

    fun getById(id: UUID): MasterProcedureEntity? {
        return masterProcedureRepository.findById(id)
    }

    fun getByMasterId(masterId: UUID): List<MasterProcedureEntity> {
        return masterProcedureRepository.findByMasterId(masterId)
    }

    fun getByProcedureId(procedureId: UUID): List<MasterProcedureEntity> {
        return masterProcedureRepository.findByProcedureId(procedureId)
    }

    fun getByMasterIdAndProcedureId(masterId: UUID, procedureId: UUID): MasterProcedureEntity? {
        return masterProcedureRepository.findByMasterIdAndProcedureId(masterId, procedureId)
    }
}
