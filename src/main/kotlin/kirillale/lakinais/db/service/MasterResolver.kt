package kirillale.lakinais.db.service

import kirillale.lakinais.db.entities.AccountFormEntity
import kirillale.lakinais.db.TestDataMarkers
import kirillale.lakinais.db.repositiries.AccountRepository
import java.util.UUID

/**
 * Определяет мастера для записи.
 * Сейчас — один мастер (из конфига или первый реальный сотрудник в БД).
 * Позже можно расширить выбором мастера клиентом.
 */
class MasterResolver(
    private val accountRepository: AccountRepository = AccountRepository(),
    private val configuredMasterId: UUID? = null,
) {
    fun resolveMasterId(): UUID {
        configuredMasterId?.let { return it }
        return findRealStaffAccount()?.id
            ?: error(
                "Мастер не найден. Укажите booking.masterId в application.yaml " +
                    "или добавьте аккаунт с ролью MASTER/OWNER (не тестовый).",
            )
    }

    private fun findRealStaffAccount(): AccountFormEntity? {
        val staffRoles = listOf("OWNER", "ADMIN", "MASTER", "M")
        for (role in staffRoles) {
            val account = accountRepository.findByRole(role).firstOrNull { !it.isTestAccount() }
            if (account != null) return account
        }
        return null
    }

    private fun AccountFormEntity.isTestAccount(): Boolean =
        TestDataMarkers.isTestTelegramId(telegramId)
}
