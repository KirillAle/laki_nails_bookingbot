package kirillale.lakinais.db

import kirillale.lakinais.config.AppEnv
import org.ktorm.database.Database

object DatabaseFactory {
    private val databaseUrl = AppEnv.get("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/postgres"
    private val databaseUser = AppEnv.get("DATABASE_USER") ?: AppEnv.get("DATABASE_NAME") ?: "postgres"
    private val databasePassword = AppEnv.get("DATABASE_PASSWORD") ?: ""

    val db by lazy {
        Database.connect(
            url = databaseUrl,
            driver = "org.postgresql.Driver",
            user = databaseUser,
            password = databasePassword,
        )
    }

    fun init() {
        db.useConnection { connection ->
            connection.isValid(3)
        }
    }
}
