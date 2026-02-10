package kirillale.lakinais.db.entities

import org.ktorm.entity.Entity
import java.time.Instant
import java.util.UUID

interface MasterTimeBlockEntity : Entity<MasterTimeBlockEntity> {

    companion object : Entity.Factory<MasterTimeBlockEntity>()

    var id: UUID
    var masterId: UUID
    var date: Instant
    var startTime: Instant
    var endTime: Instant
    var reason: String?
    var createdAt: Instant?
}
