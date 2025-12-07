package example.com.plugins

import Database
import io.ktor.server.application.Application

val db = Database


fun Application.configureKtormDbConnection() {

//        db.getConnection(
//            url = environment.config.property("DATABASE_URL").getString(),
//            driver = "org.postgresql.Driver",
//            user = environment.config.property("DATABASE_USER").getString(),
//            password = environment.config.property("DATABASE_PASSWORD").getString(),
//        )
    Database.select()
}