package example.com

import example.com.plugins.*
import configureApplicationTgBot
import io.ktor.server.application.*

//



fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

suspend fun Application.module() {
    configureSerialization()
    configureRouting()
    configureApplicationTgBot()
    configureKtormDbConnection()
}
