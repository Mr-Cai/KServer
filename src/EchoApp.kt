import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import java.io.*
import java.util.*

/**
 * Two mains are provided, you must first start EchoApp.Server, and then EchoApp.Client.
 * You can also start EchoApp.Server and then use a telnet client to connect to the echo server.
 */
object EchoApp {
    @KtorExperimentalAPI
    val selectorManager = ActorSelectorManager(Dispatchers.IO)
    const val DefaultPort = 9002

    object Server {
        @KtorExperimentalAPI
        @JvmStatic
        fun main(args: Array<String>) {
            runBlocking {
                val serverSocket = aSocket(selectorManager).tcp().bind(port = DefaultPort)
                println("Echo Server listening at ${serverSocket.localAddress}")
                while (true) {
                    val socket = serverSocket.accept()
                    println("Accepted $socket")
                    launch {
                        val read = socket.openReadChannel()
                        val write = socket.openWriteChannel(autoFlush = true)
                        try {
                            while (true) {
                                val line = read.readUTF8Line()
                                write.writeStringUtf8("$line\n")
                            }
                        } catch (e: Throwable) {
                            AutoCloseable {
                                socket.close()
                            }
                        }
                    }
                }
            }
        }
    }

    object Client {
        @KtorExperimentalAPI
        @JvmStatic
        fun main(args: Array<String>) {
            runBlocking {
                val socket = aSocket(selectorManager).tcp().connect("127.0.0.1", port = DefaultPort)
                val read = socket.openReadChannel()
                val write = socket.openWriteChannel(autoFlush = true)

                launch(Dispatchers.IO) {
                    while (true) {
                        val line = read.readUTF8Line()
                        println("server: $line")
                    }
                }

                for (line in System.`in`.lines()) {
                    println("client: $line")
                    write.writeStringUtf8("$line\n")
                }
            }
        }

        private fun InputStream.lines() = Scanner(this).lines()

        private fun Scanner.lines() = sequence {
            while (hasNext()) {
                yield(readLine())
            }
        }
    }
}
