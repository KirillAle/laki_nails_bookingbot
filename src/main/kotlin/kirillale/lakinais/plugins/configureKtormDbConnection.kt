package kirillale.lakinais.plugins

import io.ktor.server.application.Application
import kirillale.lakinais.db.DatabaseFactory
import kirillale.lakinais.db.seedProceduresIfNeeded

fun Application.configureKtormDbConnection() {
    DatabaseFactory.init()
    seedProceduresIfNeeded()
}