package kirillale.lakinais.db.service

import kirillale.lakinais.db.entities.AccountFormEntity
import kirillale.lakinais.db.repositiries.AccountRepository
import java.util.UUID

class AccountService(
    private val accountRepository: AccountRepository = AccountRepository()
) {
    fun findOrCreateTelegramUser(
        telegramId: String,
        firstName: String,
        lastName: String,
        userName: String,
    ) : AccountFormEntity {

        val existing = accountRepository.findByTelegramId(telegramId)
        if (existing != null) return existing

        return accountRepository.createAccount(
            telegramId = telegramId,
            firstName = firstName,
            lastName = lastName,
            userName = userName,
            role = "CLIENT"
        )
    }

    fun getById(id: UUID): AccountFormEntity? {
        return accountRepository.findById(id)
    }

    fun getByTelegramId(telegramId: String): AccountFormEntity? {
        return accountRepository.findByTelegramId(telegramId)
    }

    fun getByRole(role: String): List<AccountFormEntity> {
        return accountRepository.findByRole(role)
    }
}