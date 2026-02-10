package kirillale.lakinais.db.repositiries

import kirillale.lakinais.db.DatabaseFactory
import kirillale.lakinais.db.entities.AccountFormEntity
import kirillale.lakinais.db.tables.AccountFormTable
import org.ktorm.dsl.eq
import org.ktorm.entity.*
import java.time.Instant
import java.util.UUID

class AccountRepository {

    private val db = DatabaseFactory.db

    fun findById(id: UUID): AccountFormEntity? {
        return db.sequenceOf(AccountFormTable)
            .firstOrNull { it.id eq id }
    }

    fun findByTelegramId(telegramId: String): AccountFormEntity? {
        return db.sequenceOf(AccountFormTable)
            .firstOrNull { it.telegramId eq telegramId }
    }

    fun findByRole(role: String): List<AccountFormEntity> {
        return db.sequenceOf(AccountFormTable)
            .filter { it.role eq role }
            .toList()
    }

    fun createAccount(
        telegramId: String,
        firstName: String,
        lastName: String,
        userName: String?,
        role: String
    ): AccountFormEntity {

        val entity = AccountFormEntity {
            this.id = UUID.randomUUID()
            this.telegramId = telegramId
            this.firstName = firstName
            this.lastName = lastName
            this.userName = userName ?: ""
            this.role = role
            createdAt = Instant.now()
        }

        db.sequenceOf(AccountFormTable).add(entity)
        return entity
    }
}
