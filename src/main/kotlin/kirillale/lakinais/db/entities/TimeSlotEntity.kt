package kirillale.lakinais.db.entities

import org.ktorm.entity.Entity
import java.util.UUID

interface TimeSlotEntity : Entity<TimeSlotEntity> {

    companion object : Entity.Factory<TimeSlotEntity>()

    var id: UUID
    var minutes: Int
}
