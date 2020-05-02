import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val env = applicationEngineEnvironment {
        module {
            main()
        }
        // Private API
        connector {
            host = "127.0.0.1"
            port = 9090
        }
        // Public API
        connector {
            host = "0.0.0.0"
            port = 80
        }
    }
    embeddedServer(Netty, env).start(true)
}

fun Application.main() {
    routing {
        get("/") {
            if (call.request.local.port == 80) {
                call.respondText("公网API")
            } else {
                call.respondText("内网API")
            }
        }
    }
}