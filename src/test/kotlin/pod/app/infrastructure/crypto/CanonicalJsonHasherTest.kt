package pod.app.infrastructure.crypto

import org.junit.jupiter.api.Test
import pod.app.domain.record.AgentInfo
import pod.app.domain.record.Attempt
import pod.app.domain.record.Decision
import pod.app.domain.record.GENESIS_HASH
import pod.app.domain.record.HashBody
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class CanonicalJsonHasherTest {
    private val hasher = CanonicalJsonHasher()

    private val body = HashBody(
        seq = 1,
        prevHash = GENESIS_HASH,
        at = "2026-09-20T14:03:11+09:00",
        agent = AgentInfo(provider = "anthropic", model = "claude", requestId = "req_1", sessionId = "sess_1"),
        attempt = Attempt(tool = "pay", item = "계란 30구", amount = 5980, currency = "KRW", merchant = "마트A"),
        decision = Decision.BLOCKED,
        reason = "NO_PAYMENT_PERMISSION",
        rawRequest = "{}",
    )

    @Test
    fun `맵은 키 순서와 무관하게 같은 JSON`() {
        val a = linkedMapOf("b" to 1, "a" to 2)
        val b = linkedMapOf("a" to 2, "b" to 1)
        assertEquals(hasher.canonicalJson(a), hasher.canonicalJson(b))
        assertEquals("""{"a":2,"b":1}""", hasher.canonicalJson(a))
    }

    @Test
    fun `sha256 알려진 값`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            hasher.sha256Hex("abc"),
        )
    }

    @Test
    fun `HashBody는 중첩까지 알파벳순, 공백 없이, enum은 이름으로 직렬화된다`() {
        val expected = """{"agent":{"model":"claude","provider":"anthropic","requestId":"req_1","sessionId":"sess_1"},""" +
            """"at":"2026-09-20T14:03:11+09:00",""" +
            """"attempt":{"amount":5980,"currency":"KRW","item":"계란 30구","merchant":"마트A","tool":"pay"},""" +
            """"decision":"BLOCKED","prevHash":"$GENESIS_HASH","rawRequest":"{}","reason":"NO_PAYMENT_PERMISSION","seq":1}"""
        assertEquals(expected, hasher.canonicalJson(body))
        assertEquals(hasher.sha256Hex(expected), hasher.hashOf(body))
    }

    @Test
    fun `금액이 1원만 달라도 지문이 달라진다`() {
        val changed = body.copy(attempt = body.attempt.copy(amount = 5981))
        assertNotEquals(hasher.hashOf(body), hasher.hashOf(changed))
    }
}
