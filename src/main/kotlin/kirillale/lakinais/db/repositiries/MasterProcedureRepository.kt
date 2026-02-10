package kirillale.lakinais.db.repositiries

import kirillale.lakinais.db.DatabaseFactory
import kirillale.lakinais.db.entities.MasterProcedureEntity
import kirillale.lakinais.db.tables.MasterProcedureTable
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.entity.*
import java.util.UUID

class MasterProcedureRepository {

    private val db = DatabaseFactory.db

    fun findById(id: UUID): MasterProcedureEntity? {
        return db.sequenceOf(MasterProcedureTable)
            .firstOrNull { it.id eq id }
    }

    fun findByMasterId(masterId: UUID): List<MasterProcedureEntity> {
        return db.sequenceOf(MasterProcedureTable)
            .filter { it.master_id eq masterId }
            .toList()
    }

    fun findByProcedureId(procedureId: UUID): List<MasterProcedureEntity> {
        return db.sequenceOf(MasterProcedureTable)
            .filter { it.procedure_id eq procedureId }
            .toList()
    }

    fun findByMasterIdAndProcedureId(masterId: UUID, procedureId: UUID): MasterProcedureEntity? {
        return db.sequenceOf(MasterProcedureTable)
            .firstOrNull { 
                (it.master_id eq masterId) and (it.procedure_id eq procedureId)
            }
    }

    fun createMasterProcedure(
        masterId: UUID,
        procedureId: UUID
    ): MasterProcedureEntity {
        val entity = MasterProcedureEntity {
            id = UUID.randomUUID()
            this.masterId = masterId
            this.procedureId = procedureId
        }

        db.sequenceOf(MasterProcedureTable).add(entity)
        return entity
    }
}
