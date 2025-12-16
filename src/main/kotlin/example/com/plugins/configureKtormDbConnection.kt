package example.com.plugins

import example.com.Database
import io.ktor.server.application.Application


fun Application.configureKtormDbConnection() {
    Database.select()
}