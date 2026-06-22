package kirillale.lakinais.db.service

import kirillale.lakinais.db.entities.ProcedureEntity
import kirillale.lakinais.db.repositiries.ProcedureRepository
import java.util.UUID

class ProcedureService(
    private val procedureRepository: ProcedureRepository = ProcedureRepository()
) {

    fun createProcedure(
        procedureType: String,
        procedureSubtype: String,
        durationSlot: Int,
        price: Int,
        isActive: Boolean = true
    ): ProcedureEntity {
        return procedureRepository.createProcedure(
            procedureType = procedureType,
            procedureSubtype = procedureSubtype,
            durationSlot = durationSlot,
            price = price,
            isActive = isActive
        )
    }

    fun getById(id: UUID): ProcedureEntity? {
        return procedureRepository.findById(id)
    }

    fun getByType(procedureType: String): List<ProcedureEntity> {
        return procedureRepository.findByType(procedureType)
    }

    fun getBySubtype(procedureSubtype: String): List<ProcedureEntity> {
        return procedureRepository.findBySubtype(procedureSubtype)
    }

    fun getByTypeAndSubtype(procedureType: String, procedureSubtype: String): ProcedureEntity? {
        return procedureRepository.findByTypeAndSubtype(procedureType, procedureSubtype)
    }

    fun getActiveProcedures(): List<ProcedureEntity> {
        return procedureRepository.findActive()
    }

    fun activateProcedure(id: UUID): ProcedureEntity? {
        return procedureRepository.updateActiveStatus(id, true)
    }

    fun deactivateProcedure(id: UUID): ProcedureEntity? {
        return procedureRepository.updateActiveStatus(id, false)
    }
}
