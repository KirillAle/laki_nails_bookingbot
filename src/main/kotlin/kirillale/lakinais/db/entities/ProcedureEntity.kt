package kirillale.lakinais.db.entities

import org.ktorm.entity.Entity
import java.util.UUID

interface ProcedureEntity : Entity<ProcedureEntity> {

    companion object : Entity.Factory<ProcedureEntity>()

    var id: UUID
    var procedureType: String
    var procedureSubtype: String
    var durationSlot: Int
    var price: Int
    var isActive: Boolean
}
