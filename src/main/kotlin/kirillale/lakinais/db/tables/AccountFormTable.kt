package kirillale.lakinais.db.tables

import kirillale.lakinais.db.entities.AccountFormEntity
import org.ktorm.schema.Table
import org.ktorm.schema.timestamp
import org.ktorm.schema.uuid
import org.ktorm.schema.varchar

object AccountFormTable: Table<AccountFormEntity>("account_form"){

    val id = uuid("id")
        .primaryKey()
        .bindTo(AccountFormEntity::id)

    val telegramId = varchar("telegram_id")
        .bindTo(AccountFormEntity::telegramId)

    val firstName = varchar("first_name")
        .bindTo(AccountFormEntity::firstName)

    val lastName = varchar("last_name")
        .bindTo(AccountFormEntity::lastName)

    val userName = varchar("user_name")
        .bindTo(AccountFormEntity::userName)

    val role = varchar("role")
        .bindTo(AccountFormEntity::role)

    val createdAt = timestamp("created_at")
        .bindTo(AccountFormEntity::createdAt)
}