package kirillale.lakinais.db.entities

import org.ktorm.entity.Entity
import java.time.Instant
import java.util.UUID

interface AccountFormEntity : Entity<AccountFormEntity> {

    companion object : Entity.Factory<AccountFormEntity>()

    var id: UUID
    var telegramId: String
    var firstName: String
    var lastName: String
    var userName: String
    var role: String
    var phone: String?
    var createdAt: Instant
}