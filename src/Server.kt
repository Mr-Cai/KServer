import com.auth0.jwt.*
import com.auth0.jwt.algorithms.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.network.tls.certificates.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import org.apache.http.auth.*
import java.io.*
import java.text.*
import java.util.*

@KtorExperimentalAPI
fun main(args: Array<String>) {
    // 生成SSL证书
    val file = File("./www.funrefresh.com.jks")

    if (!file.exists()) {
        file.parentFile.mkdirs()
        generateCertificate(file)
    }

    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

@KtorExperimentalAPI
@Suppress("unused")
fun Application.apiModule() {
    val simpleJwt = SimpleJWT(secret = "my-super-secret-for-jwt")

    val root = File("web").takeIf { it.exists() }
        ?: File("files").takeIf { it.exists() }
        ?: error("找不到文件或文件夹")

    install(AutoHeadResponse)   // 自动响应请求头

    install(CallLogging)

    install(HSTS)

    install(HttpsRedirect) {
        sslPort = 443
        permanentRedirect = true
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        allowCredentials = true
        anyHost()
    }

    install(StatusPages) {
        exception<InvalidCredentialsException> { exception ->
            call.respond(HttpStatusCode.Unauthorized, mapOf("OK" to false, "error" to (exception.message ?: "")))
        }
    }

    install(Authentication) {
        jwt {
            verifier(simpleJwt.verifier)
            validate {
                UserIdPrincipal(it.payload.getClaim("name").asString())
            }
        }
    }

    install(ContentNegotiation) {
        gson {
            enableComplexMapKeySerialization()
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
        }
    }

    routing {
        get("/") {
            call.respondFile(File("web/index.html"))
        }

        route("/") {
            files(root)
        }

        route("/files") {
            files(root)
            listing(root)
        }

        routePath()

        post("/login-register") {
            val post = call.receive<LoginRegister>()
            val user = users.getOrPut(post.user) { User(post.user, post.password) }
            if (user.password != post.password) throw InvalidCredentialsException("Invalid credentials")
            call.respond(mapOf("token" to simpleJwt.sign(user.name)))
        }

        get("/info") {
            call.respondInfo()
        }

        route("/snippets") {
            get {
                call.respond(mapOf("snippets" to synchronized(snippets) { snippets.toList() }))
            }
            authenticate {
                post {
                    val post = call.receive<PostSnippet>()
                    val principal = call.principal<UserIdPrincipal>() ?: error("No principal")
                    snippets += Snippet(principal.name, post.snippet.text)
                    call.respond(mapOf("OK" to true))
                }
            }
        }
    }
}

fun Route.routePath() {
    get("/assets/svg/ic_launcher.svg") {
        call.respondFile(File("/home/assets/svg/ic_launcher.svg"))
    }
}

data class PostSnippet(val snippet: Text) {
    data class Text(val text: String)
}

data class Snippet(val user: String, val text: String)

val snippets: MutableList<Snippet> = Collections.synchronizedList(
    mutableListOf(
        Snippet(user = "test", text = "hello"),
        Snippet(user = "test", text = "world")
    )
)

open class SimpleJWT(secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier: JWTVerifier = JWT.require(algorithm).build()
    fun sign(name: String): String = JWT.create().withClaim("name", name).sign(algorithm)
}

class User(val name: String, val password: String)

val users: MutableMap<String, User> = Collections.synchronizedMap(
    listOf(User("test", "test"))
        .associateBy { it.name }
        .toMutableMap()
)

class LoginRegister(val user: String, val password: String)