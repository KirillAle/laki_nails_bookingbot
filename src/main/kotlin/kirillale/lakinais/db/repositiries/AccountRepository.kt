package kirillale.lakinais.db.repositiries

import kirillale.lakinais.db.DatabaseFactory
import kirillale.lakinais.db.entities.AccountFormEntity
import kirillale.lakinais.db.tables.AccountFormTable
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.dsl.like
import org.ktorm.dsl.or
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

    fun findByUserName(userName: String): AccountFormEntity? {
        val normalized = normalizeUserName(userName)
        if (normalized.isEmpty()) return null
        return db.sequenceOf(AccountFormTable)
            .toList()
            .firstOrNull { normalizeUserName(it.userName) == normalized }
    }

    fun updateTelegramId(id: UUID, telegramId: String): AccountFormEntity? {
        val existing = findById(id) ?: return null
        existing.telegramId = telegramId
        existing.flushChanges()
        return existing
    }

    fun deleteById(id: UUID): Boolean {
        val existing = findById(id) ?: return false
        existing.delete()
        return true
    }

    private fun normalizeUserName(userName: String): String =
        userName.trim().removePrefix("@").lowercase()

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
            this.phone = null
            createdAt = Instant.now()
        }

        db.sequenceOf(AccountFormTable).add(entity)
        return entity
    }

    fun updatePhone(id: UUID, phone: String): AccountFormEntity? {
        val existing = findById(id) ?: return null
        existing.phone = phone
        existing.flushChanges()
        return existing
    }

    fun updateRole(id: UUID, role: String): AccountFormEntity? {
        val existing = findById(id) ?: return null
        existing.role = role.trim().uppercase()
        existing.flushChanges()
        return existing
    }

    fun findByPhone(phone: String): AccountFormEntity? =
        db.sequenceOf(AccountFormTable).firstOrNull { it.phone eq phone }

    fun searchClients(query: String, limit: Int = 10): List<AccountFormEntity> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return db.sequenceOf(AccountFormTable)
            .filter { it.role eq "CLIENT" }
            .filter {
                (it.phone eq trimmed) or
                    (it.telegramId eq trimmed) or
                    (it.firstName like "%$trimmed%") or
                    (it.lastName like "%$trimmed%") or
                    (it.userName like "%$trimmed%")
            }
            .take(limit)
            .toList()
    }
}
