package kirillale.lakinais.db.tables

import kirillale.lakinais.db.entities.MasterProcedureEntity
import org.ktorm.schema.Table
import org.ktorm.schema.uuid

object MasterProcedureTable : Table<MasterProcedureEntity>("master_procedure") {
    val id = uuid("id")
        .primaryKey()
        .bindTo(MasterProcedureEntity::id)

    val master_id = uuid("master_id")
        .bindTo(MasterProcedureEntity::masterId)

    val procedure_id = uuid("procedure_id")
        .bindTo(MasterProcedureEntity::procedureId)
}
