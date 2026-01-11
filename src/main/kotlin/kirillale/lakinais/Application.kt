package kirillale.lakinais

import io.ktor.server.application.*
import io.ktor.server.netty.EngineMain
import kirillale.lakinais.plugins.configureApplicationTgBot
import kirillale.lakinais.plugins.configureKtormDbConnection
import kirillale.lakinais.plugins.configureRouting
import kirillale.lakinais.plugins.configureSerialization

//



fun main(args: Array<String>) {
    EngineMain.main(args)
}

suspend fun Application.module() {
    configureSerialization()
    configureRouting()
    configureApplicationTgBot()
    configureKtormDbConnection()
}
