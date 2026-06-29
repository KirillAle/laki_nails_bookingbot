package example.com

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kirillale.lakinais.plugins.configureRouting
import kotlin.test.*

class ApplicationTest {
    @Test
    fun testHealth() = testApplication {
        application {
            configureRouting()
        }
        client.get("/health").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("ok", bodyAsText())
        }
    }
}
