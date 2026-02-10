package kirillale.lakinais.db.entities

import org.ktorm.entity.Entity
import java.util.UUID

interface MasterProcedureEntity : Entity<MasterProcedureEntity> {

    companion object : Entity.Factory<MasterProcedureEntity>()

    var id: UUID
    var masterId: UUID
    var procedureId: UUID
}
