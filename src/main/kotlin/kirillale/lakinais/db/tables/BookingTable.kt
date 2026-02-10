package kirillale.lakinais.db.tables

import kirillale.lakinais.db.entities.BookingEntity
import org.ktorm.schema.Table
import org.ktorm.schema.timestamp
import org.ktorm.schema.uuid
import org.ktorm.schema.varchar

object BookingTable: Table<BookingEntity>("booking"){
    val id = uuid("id")
    .primaryKey()
    .bindTo(BookingEntity::id)
    
    val client_id = uuid("client_id")
    .bindTo(BookingEntity::clientId)

    val master_id = uuid("master_id")
    .bindTo(BookingEntity::masterId)

    val schedule_id = uuid("schedule_id")
    .bindTo(BookingEntity::scheduleId)

    val procedure_id = uuid("procedure_id")
    .bindTo(BookingEntity::procedureId)

    val status = varchar("status")
    .bindTo(BookingEntity::statusName)

    val created_at = timestamp ("created_at")
    .bindTo(BookingEntity::createdAt)
}