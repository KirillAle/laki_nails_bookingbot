package kirillale.lakinais.plugins

import kirillale.lakinais.db.DatabaseFactory
import io.ktor.server.application.Application


fun Application.configureKtormDbConnection() {
    DatabaseFactory.select()
}