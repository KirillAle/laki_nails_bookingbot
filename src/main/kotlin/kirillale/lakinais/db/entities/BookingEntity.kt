package kirillale.lakinais.db.entities

import org.ktorm.entity.Entity
import java.time.Instant
import java.util.UUID


interface BookingEntity: Entity<BookingEntity> {

    companion object: Entity.Factory<BookingEntity>()

    var id: UUID
    var clientId: UUID
    var masterId: UUID
    var scheduleId: UUID
    var procedureId: UUID
    var statusName: String
    var createdAt: Instant?
}