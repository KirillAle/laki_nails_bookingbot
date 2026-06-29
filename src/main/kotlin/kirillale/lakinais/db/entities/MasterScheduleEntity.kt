package kirillale.lakinais.db.entities

import org.ktorm.entity.Entity
import java.time.Instant
import java.util.UUID

interface MasterScheduleEntity : Entity<MasterScheduleEntity> {

    companion object : Entity.Factory<MasterScheduleEntity>()

    var id: UUID
    var masterId: UUID
    var date: Instant
    var timeStart: Instant
    var timeEnd: Instant
    var breakStart: Instant
    var breakEnd: Instant
    var isOpen: Boolean
}
