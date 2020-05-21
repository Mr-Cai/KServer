@file:Suppress("SpellCheckingInspection")

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import database.Info
import database.md5
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.jwt.jwt
import io.ktor.auth.principal
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.files
import io.ktor.http.content.streamProvider
import io.ktor.network.tls.certificates.generateCertificate
import io.ktor.request.isMultipart
import io.ktor.request.receive
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.response.respondFile
import io.ktor.response.respondTextWriter
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.commandLineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.apache.http.auth.InvalidCredentialsException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


@KtorExperimentalAPI
fun main(args: Array<String>) {
    // 绑定SSL证书
    val file = File("./www.funrefresh.com.jks")

    // 生成SSL证书
    if (!file.exists()) {
        file.parentFile.mkdirs()
        generateCertificate(file)
    }

    Database.connect(
        url = "jdbc:mysql://106.53.106.142:3306/users",
        driver = "com.mysql.cj.jdbc.Driver",
        user = "root",
        password = "153580"
    )

    embeddedServer(Netty, commandLineEnvironment(args)).start(wait = true)
}

@KtorExperimentalAPI
@Suppress("unused")
fun Application.apiModule() {
    val secretJWT = SecretJWT(secret = "json-web-token-secret")

    val webRoot = File("./web/").takeIf { it.exists() }
        ?: error("找不到文件或文件夹")

    val root = File("./").takeIf { it.exists() }
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
            call.respond(
                HttpStatusCode.Unauthorized,
                mapOf(
                    "OK" to false,
                    "error" to (exception.message ?: "")
                )
            )
        }
    }

    install(Authentication) {
        jwt {
            verifier(secretJWT.verifier)
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
        route("/") {
            get { call.respondFile(File("./web/index.html")) }
            files(webRoot)
            listing(webRoot)
            files(root)
            listing(root)
        }

        route("/l") {
            files(root)
            listing(root)
        }

        post("/login") {
            val param = call.receive<UserInfo>()
            var loginMsg = ""

            transaction {
                when {
                    param.phone.isNotEmpty() && param.password.isNotEmpty()
                    -> {
                        Info.select {
                            Info.phone eq param.phone
                        }.run {
                            if (count() == 0L) {
                                loginMsg = "手机号未注册"
                            }
                            forEach {
                                loginMsg = when {
                                    param.phone == it[Info.phone] &&
                                            param.password == it[Info.password]
                                    -> "登录成功"

                                    param.password != it[Info.password] -> {
                                        "密码输入有误"
                                    }

                                    else -> "登录参数缺失"
                                }
                            }
                        }
                    }

                    param.nick_name.isNotEmpty() && param.password.isNotEmpty()
                    -> {
                        Info.select {
                            Info.nick_name eq param.nick_name
                        }.run {
                            if (count() == 0L) {
                                loginMsg = "用户不存在"
                            }
                            forEach {
                                loginMsg = when {
                                    param.nick_name == it[Info.nick_name] &&
                                            param.password == it[Info.password]
                                    -> "登录成功"

                                    param.password != it[Info.password] -> {
                                        "密码输入有误"
                                    }

                                    else -> "登录参数缺失"
                                }
                            }
                        }

                    }

                    else -> loginMsg = "登录参数缺失"
                }
            }
            call.respond(mapOf("result" to loginMsg))
        }

        get("/info") {
            call.respondInfo()
        }

        route("/users") {
            get {
                call.respond(mapOf("users" to synchronized(getUsers()) { getUsers() }))
            }

            authenticate {
                post {
                    val post = call.receive<UserInfo>()
                    val principal = call.principal<UserIdPrincipal>() ?: error("No principal")
                    getUsers().add(UserInfo(nick_name = principal.name, real_name = post.real_name))
                    call.respond(mapOf("OK" to true))
                }
            }
        }

        post("/register") {
            val config = environment.config.config("ktor")
            val uploadPath = config.property("upload.dir").getString()
            val uploadDir = File(uploadPath)

            if (!uploadDir.exists() && !uploadDir.mkdirs()) {
                throw IOException("创建目录失败 : ${uploadDir.absolutePath}")
            }

            val formMap = HashMap<String, String>()

            val multipart = call.receiveMultipart()

            call.respondTextWriter {
                if (!call.request.isMultipart()) {
                    appendln("不是分段请求")
                } else {
                    while (true) {
                        val part = multipart.readPart() ?: break
                        when (part) {
                            is PartData.FormItem -> {
                                formMap["${part.name}"] = part.value
                            }
                            is PartData.FileItem -> {
                                val file = File(
                                    uploadDir,
                                    "${part.originalFileName}"
                                )
                                part.streamProvider().use { its ->
                                    file.outputStream().buffered().use {
                                        its.copyToSuspend(it)
                                    }
                                }

                                val source: Path = Paths.get(file.path)
                                val headPath: Path = source.resolveSibling(
                                    "md5Id/avatar/header.png"
                                )

                                withContext(Dispatchers.IO) {
                                    if (Files.notExists(headPath)) {
                                        Files.createDirectories(headPath)
                                    }
                                    Files.move(
                                        source, headPath,
                                        StandardCopyOption.REPLACE_EXISTING
                                    ).run {
                                        transaction {
                                            try {
                                                Info.update({
                                                    Info.id eq "cdabbf2440a26771"
                                                }) {
                                                    it[avatar] = "https://funrefresh.com/$headPath"
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                        appendln(
                                            Gson().toJson(
                                                mapOf("avatar_link" to "https://funrefresh.com/$headPath")
                                            )
                                        )
                                    }
                                }
                            }
                            is PartData.BinaryItem ->
                                appendln("BinaryItem: ${part.name} -> ${part.provider}")
                        }
                        part.dispose()
                    }
                }
            }

            transaction {
                try {
                    if (formMap.isNotEmpty()) {
                        val count = Info.select {
                            Info.id eq "${formMap["nick_name"]}".md5()
                        }.count()
                        if (count != 1L) {
                            Info.insert {
                                it[id] = "${formMap["nick_name"]}".md5()
                                it[nick_name] = "${formMap["nick_name"]}"
                                it[phone] = "${formMap["phone"]}"
                                it[password] = "${formMap["password"]}"
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

suspend fun InputStream.copyToSuspend(
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
    yieldSize: Int = 4 * 1024 * 1024
): Long {
    return withContext(Dispatchers.IO) {
        val buffer = ByteArray(bufferSize)
        var bytesCopied = 0L
        var bytesAfterYield = 0L
        while (true) {
            val bytes = read(buffer).takeIf { it >= 0 } ?: break
            out.write(buffer, 0, bytes)
            if (bytesAfterYield >= yieldSize) {
                yield()
                bytesAfterYield %= yieldSize
            }
            bytesCopied += bytes
            bytesAfterYield += bytes
        }
        return@withContext bytesCopied
    }
}


data class UserInfo(
    val id: String = "",
    val avatar: String = "",
    val nick_name: String = "",
    val real_name: String = "",
    val password: String = "",
    val age: Int = 0,
    val gender: String = "",
    val phone: String = "",
    val job: String = "",
    val love: String = "",
    val qq: String = "",
    val wechat: String = ""
)

//data class LoginParam(
//    val userName: String = "",
//    val password: String = "",
//    val phone: String = ""
//)

fun getUsers(): MutableList<UserInfo> {
    val list = ArrayList<UserInfo>()

    transaction {
        Info.selectAll().map {
            list.add(
                UserInfo(
                    id = it[Info.id],
                    avatar = it[Info.avatar],
                    nick_name = it[Info.nick_name],
                    real_name = it[Info.real_name],
                    age = it[Info.age],
                    gender = it[Info.gender],
                    phone = it[Info.phone],
                    job = it[Info.job],
                    love = it[Info.love],
                    qq = it[Info.qq],
                    wechat = it[Info.wechat]
                )
            )
        }
    }

    Collections.synchronizedList(list)

    return list
}

open class SecretJWT(val secret: String) {
    private val algorithm = Algorithm.HMAC256(secret)
    val verifier: JWTVerifier = JWT.require(algorithm).build()
    fun sign(name: String): String = JWT.create().withClaim("name", name).sign(algorithm)
}
