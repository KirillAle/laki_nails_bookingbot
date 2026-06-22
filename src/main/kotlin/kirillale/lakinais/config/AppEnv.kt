package kirillale.lakinais.config

import io.github.cdimascio.dotenv.dotenv

object AppEnv {
    private val dotenv = try {
        dotenv {
            directory = System.getProperty("user.dir")
            filename = "environment.env"
            ignoreIfMissing = true
        }
    } catch (_: Exception) {
        null
    }

    fun get(key: String): String? = System.getenv(key) ?: dotenv?.get(key)
}
