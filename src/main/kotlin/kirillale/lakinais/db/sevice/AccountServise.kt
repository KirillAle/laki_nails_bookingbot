package kirillale.lakinais.db.sevice

import kirillale.lakinais.db.entities.AccountFormEntity
import kirillale.lakinais.db.repositiries.AccountRepository

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

        return accountRepository.create(
            telegramId = telegramId,
            firstName = firstName,
            lastName = lastName,
            userName = userName,
            role = "CLIENT"
        )
    }

}