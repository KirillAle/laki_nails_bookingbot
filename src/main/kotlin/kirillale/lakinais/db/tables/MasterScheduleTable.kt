package kirillale.lakinais.db.tables

import kirillale.lakinais.db.entities.MasterScheduleEntity
import org.ktorm.schema.Table
import org.ktorm.schema.timestamp
import org.ktorm.schema.uuid

object MasterScheduleTable : Table<MasterScheduleEntity>("master_schedule") {
    val id = uuid("id")
        .primaryKey()
        .bindTo(MasterScheduleEntity::id)

    val master_id = uuid("master_id")
        .bindTo(MasterScheduleEntity::masterId)

    val date = timestamp("date")
        .bindTo(MasterScheduleEntity::date)

    val time_start = timestamp("time_start")
        .bindTo(MasterScheduleEntity::timeStart)

    val time_end = timestamp("time_end")
        .bindTo(MasterScheduleEntity::timeEnd)

    val break_start = timestamp("break_start")
        .bindTo(MasterScheduleEntity::breakStart)

    val break_end = timestamp("break_end")
        .bindTo(MasterScheduleEntity::breakEnd)
}
