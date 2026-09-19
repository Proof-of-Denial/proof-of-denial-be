package pod.app.infrastructure.llm

import com.anthropic.core.JsonValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import pod.app.domain.record.AgentInfo
import pod.app.domain.record.Attempt
import pod.app.domain.record.Decision
import pod.app.domain.record.RecordService
import pod.app.infrastructure.crypto.CanonicalJsonHasher
import pod.app.infrastructure.crypto.Ed25519Keys
import pod.app.infrastructure.crypto.Ed25519Signer
import pod.app.infrastructure.repository.record.persistence.RecordRepositoryImpl
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import java.nio.file.Path
import java.time.Clock
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * AI 도구 호출 입력이 장부의 rawRequest로 들어갈 때 반드시 JSON이어야 한다.
 * JsonValue.toString()은 자바 Map 표기({item=계란})를 내놓기 때문에 jsonMapper()로 직렬화한다.
 * 이 경로는 API 키 없이는 실제로 돌려볼 수 없어서, 직렬화 결과만이라도 테스트로 못 박는다.
 */
class ToolInputJsonTest {

    private val payInput: JsonValue = JsonValue.from(
        mapOf(
            "item" to "계란 30구",
            "amount" to 5980,
            "merchant" to "마트A",
        ),
    )

    @Test
    fun `도구 입력은 자바 Map 표기가 아니라 JSON으로 직렬화된다`() {
        val rawRequest = toolInputToJson(payInput)

        assertTrue(rawRequest.startsWith("{\""), "JSON이어야 한다. 실제: $rawRequest")
        assertTrue(!rawRequest.contains("item="), "자바 Map 표기가 남아 있다: $rawRequest")
        assertTrue(rawRequest.contains("\"item\":\"계란 30구\""), "실제: $rawRequest")
        assertTrue(rawRequest.contains("\"amount\":5980"), "금액은 따옴표 없는 숫자여야 한다. 실제: $rawRequest")
        assertTrue(rawRequest.contains("\"merchant\":\"마트A\""), "실제: $rawRequest")
    }

    @Test
    fun `직렬화된 입력은 다시 파싱된다`() {
        val rawRequest = toolInputToJson(payInput)

        val mapper = JsonMapper.builder().addModule(kotlinModule()).build()
        val parsed = mapper.readTree(rawRequest)

        assertEquals("계란 30구", parsed.get("item").stringValue())
        assertEquals(5980L, parsed.get("amount").asLong())
        assertEquals("마트A", parsed.get("merchant").stringValue())
    }

    @Test
    fun `따옴표와 한글이 섞인 입력도 장부에 저장했다 그대로 읽힌다`(@TempDir dir: Path) {
        val trickyInput = JsonValue.from(
            mapOf(
                "item" to "계란 \"특란\" 30구",
                "amount" to 5980,
                "merchant" to "마트A\n다음줄",
            ),
        )
        val rawRequest = toolInputToJson(trickyInput)

        val keyPair = Ed25519Keys.generate()
        val recordService = RecordService(
            RecordRepositoryImpl(dir.resolve("ledger.jsonl").toString()),
            CanonicalJsonHasher(),
            Ed25519Signer(keyPair.private),
            Clock.system(ZoneId.of("Asia/Seoul")),
        )
        val agent = AgentInfo("anthropic", "claude-opus-5", "msg_01", "sess_01")
        val attempt = Attempt("pay", "계란 \"특란\" 30구", 5980, "KRW", "마트A")

        val saved = recordService.append(agent, attempt, Decision.BLOCKED, "NO_PAYMENT_PERMISSION", rawRequest)
        val reloaded = recordService.findAll()

        assertEquals(1, reloaded.size)
        assertEquals(rawRequest, reloaded[0].rawRequest)
        assertEquals(saved.hash, reloaded[0].hash)
    }
}
