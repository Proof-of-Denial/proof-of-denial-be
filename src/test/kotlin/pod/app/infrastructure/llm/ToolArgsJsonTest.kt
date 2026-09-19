package pod.app.infrastructure.llm

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/** 장부의 rawRequest는 "AI가 보낸 요청 원문"이라 반드시 JSON이어야 한다. Map.toString()이 새어 나가면 증거로서 값이 떨어진다. */
class ToolArgsJsonTest {

    private val args: Map<String, Any> = linkedMapOf(
        "item" to "계란 \"특란\" 30구",
        "amount" to 5980,
        "merchant" to "마트A",
    )

    @Test
    fun `도구 인자는 자바 Map 표기가 아니라 JSON으로 직렬화된다`() {
        val rawRequest = toolArgsToJson(args)

        assertTrue(rawRequest.startsWith("{\""), "JSON이어야 한다. 실제: $rawRequest")
        assertTrue(!rawRequest.contains("item="), "자바 Map 표기가 남아 있다: $rawRequest")
        assertTrue(rawRequest.contains("\"amount\":5980"), "금액은 따옴표 없는 숫자여야 한다. 실제: $rawRequest")
        assertTrue(rawRequest.contains("\\\"특란\\\""), "따옴표가 이스케이프돼야 한다. 실제: $rawRequest")
    }

    @Test
    fun `빈 인자도 빈 JSON 객체가 된다`() {
        assertTrue(toolArgsToJson(emptyMap()) == "{}")
    }
}
