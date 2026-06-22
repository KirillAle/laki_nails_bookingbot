package kirillale.lakinais.db.service

import kirillale.lakinais.db.entities.AccountFormEntity
import kirillale.lakinais.db.repositiries.AccountRepository
import kirillale.lakinais.domain.role.UserRole
import java.util.UUID

class AccountService(
    private val accountRepository: AccountRepository = AccountRepository(),
) {
    /**
     * Находит или создаёт аккаунт по Telegram ID.
     * Если в БД аккаунт был создан вручную с username вместо числового id —
     * привязывает его к реальному telegram_id при первом входе.
     */
    fun findOrCreateTelegramUser(
        telegramId: String,
        firstName: String,
        lastName: String,
        userName: String,
    ): AccountFormEntity {
        val byTelegramId = accountRepository.findByTelegramId(telegramId)
        val byUserName = accountRepository.findByUserName(userName)

        if (byUserName != null && byTelegramId != null && byUserName.id != byTelegramId.id) {
            return mergeDuplicateAccounts(
                telegramId = telegramId,
                staffCandidate = byUserName,
                telegramCandidate = byTelegramId,
            )
        }

        if (byUserName != null) {
            return syncTelegramIdIfNeeded(byUserName, telegramId)
        }

        if (byTelegramId != null) {
            return byTelegramId
        }

        return accountRepository.createAccount(
            telegramId = telegramId,
            firstName = firstName,
            lastName = lastName,
            userName = userName,
            role = "CLIENT",
        )
    }

    private fun mergeDuplicateAccounts(
        telegramId: String,
        staffCandidate: AccountFormEntity,
        telegramCandidate: AccountFormEntity,
    ): AccountFormEntity {
        val staffIsStaff = UserRole.isStaff(UserRole.fromDb(staffCandidate.role))
        val telegramIsStaff = UserRole.isStaff(UserRole.fromDb(telegramCandidate.role))

        val keeper = when {
            staffIsStaff && !telegramIsStaff -> staffCandidate
            telegramIsStaff && !staffIsStaff -> telegramCandidate
            else -> staffCandidate
        }
        val duplicate = if (keeper.id == staffCandidate.id) telegramCandidate else staffCandidate

        accountRepository.deleteById(duplicate.id)
        return syncTelegramIdIfNeeded(keeper, telegramId)
    }

    private fun syncTelegramIdIfNeeded(account: AccountFormEntity, telegramId: String): AccountFormEntity {
        if (account.telegramId == telegramId) return account
        return accountRepository.updateTelegramId(account.id, telegramId) ?: account
    }

    fun getById(id: UUID): AccountFormEntity? = accountRepository.findById(id)

    fun getByTelegramId(telegramId: String): AccountFormEntity? =
        accountRepository.findByTelegramId(telegramId)

    fun getByRole(role: String): List<AccountFormEntity> = accountRepository.findByRole(role)

    fun updatePhone(accountId: UUID, phone: String): AccountFormEntity? =
        accountRepository.updatePhone(accountId, phone)

    fun searchClients(query: String, limit: Int = 10) =
        accountRepository.searchClients(query, limit)
}
