package example.com.plugins

import Database
import io.ktor.server.application.Application


fun Application.configureKtormDbConnection() {
    Database.select()
}