package ktor.funny

import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlin.test.*

class ApplicationTest {
    @Test
    fun testClientMock() {
        runBlocking {
            val client = HttpClient(MockEngine) {
                engine {
                    addHandler { request ->
                        when (request.url.fullPath) {
                            "/" -> respond(
                                ByteReadChannel(byteArrayOf(1, 2, 3)),
                                headers = headersOf("X-MyHeader", "MyValue")
                            )
                            else -> respond("Not Found ${request.url.encodedPath}", HttpStatusCode.NotFound)
                        }
                    }
                }
                expectSuccess = false
            }
            assertEquals(byteArrayOf(1, 2, 3).toList(), client.get<ByteArray>("/").toList())
            assertEquals("MyValue", client.request<HttpResponse>("/") {}.headers["X-MyHeader"])
            assertEquals("Not Found other/path", client.get("/other/path"))
        }
    }

    @Test
    fun testRedirectHttps() {
        withTestApplication {
            application.install(XForwardedHeaderSupport)
            application.install(HttpsRedirect)
            application.routing {
                get("/") {
                    call.respond("ok")
                }
            }


            handleRequest(HttpMethod.Get, "/") {
                addHeader(HttpHeaders.XForwardedProto, "https")
            }.let { call ->
                assertEquals(HttpStatusCode.OK, call.response.status())
            }
        }
    }
}
