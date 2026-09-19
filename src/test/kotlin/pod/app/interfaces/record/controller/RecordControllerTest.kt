package pod.app.interfaces.record.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.env.Environment
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import pod.app.domain.record.Decision
import pod.app.domain.record.GENESIS_HASH
import pod.app.infrastructure.crypto.Ed25519Keys
import pod.app.interfaces.common.CommonRes
import pod.app.interfaces.common.ResultType
import pod.app.interfaces.record.res.HeadRes
import pod.app.interfaces.record.res.RecordRes
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RecordControllerTest {

    companion object {
        private val dir: Path = Files.createTempDirectory("ledger-test")

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            val keyPair = Ed25519Keys.generate()
            Files.writeString(dir.resolve("ed25519.private"), Ed25519Keys.encodePrivate(keyPair.private))
            registry.add("ledger.file") { dir.resolve("ledger.jsonl").toString() }
            registry.add("ledger.private-key") { dir.resolve("ed25519.private").toString() }
        }
    }

    @Autowired
    lateinit var env: Environment

    private val http = HttpClient.newHttpClient()
    private val json: JsonMapper = JsonMapper.builder().addModule(kotlinModule()).build()
    private fun url(path: String) = "http://localhost:${env.getProperty("local.server.port")}$path"

    private fun post(body: String) = http.send(
        HttpRequest.newBuilder(URI(url("/api/v1/ledger/records")))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(body)).build(),
        BodyHandlers.ofString(),
    )

    private fun get(path: String) = http.send(
        HttpRequest.newBuilder(URI(url(path))).GET().build(),
        BodyHandlers.ofString(),
    )

    private val sample = """
        {"agent":{"provider":"anthropic","model":"claude","requestId":"req_1","sessionId":"s1"},
         "attempt":{"tool":"pay","item":"계란 30구","amount":5980,"currency":"KRW","merchant":"마트A"},
         "decision":"BLOCKED","reason":"NO_PAYMENT_PERMISSION","rawRequest":"{\"tool\":\"pay\"}"}
    """.trimIndent()

    @Test
    fun `POST 하면 seq·prevHash·hash·signature가 채워져 돌아오고 GET들로 조회된다`() {
        val response = post(sample)
        assertEquals(200, response.statusCode())
        val created = json.readValue<CommonRes<RecordRes>>(response.body())
        assertEquals(ResultType.SUCCESS, created.resultType)
        val record = assertNotNull(created.data)
        assertEquals(1L, record.seq)
        assertEquals(GENESIS_HASH, record.prevHash)
        assertEquals(Decision.BLOCKED, record.decision)
        assertEquals("계란 30구", record.attempt.item)
        assertEquals(64, record.hash.length)
        assertTrue(record.signature.isNotBlank())

        val all = json.readValue<CommonRes<List<RecordRes>>>(get("/api/v1/ledger/records").body())
        assertEquals(listOf(record), all.data)

        val one = json.readValue<CommonRes<RecordRes>>(get("/api/v1/ledger/records/1").body())
        assertEquals(record, one.data)

        val head = json.readValue<CommonRes<HeadRes>>(get("/api/v1/ledger/head").body())
        assertEquals(HeadRes(1, record.hash), head.data)
    }

    @Test
    fun `없는 순번은 404와 RECORD_NOT_FOUND`() {
        val response = get("/api/v1/ledger/records/99")
        assertEquals(404, response.statusCode())
        val body = json.readValue<CommonRes<Any?>>(response.body())
        assertEquals(ResultType.FAIL, body.resultType)
        assertEquals("RECORD_NOT_FOUND", body.exception?.code)
    }

    @Test
    fun `금액이 음수면 400과 VALIDATION_ERROR`() {
        val response = post(sample.replace(""""amount":5980""", """"amount":-1"""))
        assertEquals(400, response.statusCode())
        val body = json.readValue<CommonRes<Any?>>(response.body())
        assertEquals(ResultType.FAIL, body.resultType)
        assertEquals("VALIDATION_ERROR", body.exception?.code)
    }
}
