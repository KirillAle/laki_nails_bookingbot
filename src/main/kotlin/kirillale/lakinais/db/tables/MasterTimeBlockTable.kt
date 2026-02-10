package kirillale.lakinais.db.tables

import kirillale.lakinais.db.entities.MasterTimeBlockEntity
import org.ktorm.schema.Table
import org.ktorm.schema.timestamp
import org.ktorm.schema.uuid
import org.ktorm.schema.varchar

object MasterTimeBlockTable : Table<MasterTimeBlockEntity>("master_time_block") {
    val id = uuid("id")
        .primaryKey()
        .bindTo(MasterTimeBlockEntity::id)

    val master_id = uuid("master_id")
        .bindTo(MasterTimeBlockEntity::masterId)

    val date = timestamp("date")
        .bindTo(MasterTimeBlockEntity::date)

    val start_time = timestamp("start_time")
        .bindTo(MasterTimeBlockEntity::startTime)

    val end_time = timestamp("end_time")
        .bindTo(MasterTimeBlockEntity::endTime)

    val reason = varchar("reason")
        .bindTo(MasterTimeBlockEntity::reason)

    val created_at = timestamp("created_at")
        .bindTo(MasterTimeBlockEntity::createdAt)
}
