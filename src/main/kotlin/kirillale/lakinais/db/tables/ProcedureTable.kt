package kirillale.lakinais.db.tables

import kirillale.lakinais.db.entities.ProcedureEntity
import org.ktorm.schema.Table
import org.ktorm.schema.boolean
import org.ktorm.schema.int
import org.ktorm.schema.uuid
import org.ktorm.schema.varchar

object ProcedureTable : Table<ProcedureEntity>("procedure") {
    val id = uuid("id")
        .primaryKey()
        .bindTo(ProcedureEntity::id)

    val procedure_type = varchar("procedure_type")
        .bindTo(ProcedureEntity::procedureType)

    val procedure_subtype = varchar("procedure_subtype")
        .bindTo(ProcedureEntity::procedureSubtype)

    val duration_slot = int("duration_slot")
        .bindTo(ProcedureEntity::durationSlot)

    val price = int("price")
        .bindTo(ProcedureEntity::price)

    val is_active = boolean("is_active")
        .bindTo(ProcedureEntity::isActive)
}
