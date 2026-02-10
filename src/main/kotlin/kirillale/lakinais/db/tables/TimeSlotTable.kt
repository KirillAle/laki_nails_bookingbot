package kirillale.lakinais.db.tables

import kirillale.lakinais.db.entities.TimeSlotEntity
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.uuid

object TimeSlotTable : Table<TimeSlotEntity>("time_slot") {
    val id = uuid("id")
        .primaryKey()
        .bindTo(TimeSlotEntity::id)

    val minutes = int("minutes")
        .bindTo(TimeSlotEntity::minutes)
}
