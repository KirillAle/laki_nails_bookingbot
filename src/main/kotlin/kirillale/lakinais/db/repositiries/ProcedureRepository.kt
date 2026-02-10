package kirillale.lakinais.db.repositiries

import kirillale.lakinais.db.DatabaseFactory
import kirillale.lakinais.db.entities.ProcedureEntity
import kirillale.lakinais.db.tables.ProcedureTable
import org.ktorm.dsl.eq
import org.ktorm.entity.*
import java.util.UUID

class ProcedureRepository {

    private val db = DatabaseFactory.db

    fun findById(id: UUID): ProcedureEntity? {
        return db.sequenceOf(ProcedureTable)
            .firstOrNull { it.id eq id }
    }

    fun findByType(procedureType: String): List<ProcedureEntity> {
        return db.sequenceOf(ProcedureTable)
            .filter { it.procedure_type eq procedureType }
            .toList()
    }

    fun findBySubtype(procedureSubtype: String): List<ProcedureEntity> {
        return db.sequenceOf(ProcedureTable)
            .filter { it.procedure_subtype eq procedureSubtype }
            .toList()
    }

    fun findActive(): List<ProcedureEntity> {
        return db.sequenceOf(ProcedureTable)
            .filter { it.is_active eq true }
            .toList()
    }

    fun createProcedure(
        procedureType: String,
        procedureSubtype: String,
        durationSlot: Int,
        price: Int,
        isActive: Boolean = true
    ): ProcedureEntity {
        val entity = ProcedureEntity {
            id = UUID.randomUUID()
            this.procedureType = procedureType
            this.procedureSubtype = procedureSubtype
            this.durationSlot = durationSlot
            this.price = price
            this.isActive = isActive
        }

        db.sequenceOf(ProcedureTable).add(entity)
        return entity
    }

    fun updateActiveStatus(id: UUID, isActive: Boolean): ProcedureEntity? {
        val existing = findById(id) ?: return null
        existing.isActive = isActive
        existing.flushChanges()
        return existing
    }
}
