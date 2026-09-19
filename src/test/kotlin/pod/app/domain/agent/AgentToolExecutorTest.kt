package pod.app.domain.agent

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import pod.app.domain.guard.PaymentGuardService
import pod.app.domain.product.Product
import pod.app.domain.product.ProductRepository
import pod.app.domain.record.AgentInfo
import pod.app.domain.record.Decision
import pod.app.domain.record.RecordService
import pod.app.infrastructure.crypto.CanonicalJsonHasher
import pod.app.infrastructure.crypto.Ed25519Keys
import pod.app.infrastructure.crypto.Ed25519Signer
import pod.app.infrastructure.repository.record.persistence.RecordRepositoryImpl
import java.nio.file.Path
import java.time.Clock
import java.time.ZoneId
import kotlin.test.assertEquals

class AgentToolExecutorTest {

    /** 테스트용 상품 저장소. 계란만 있다. */
    private class FakeProductRepository : ProductRepository {
        override fun findByKeyword(keyword: String): List<Product> {
            if (keyword == "계란") {
                return listOf(Product("계란 30구", 5980, "마트A"))
            }
            return emptyList()
        }
    }

    private fun executor(dir: Path, paymentAllowed: Boolean): Pair<AgentToolExecutor, RecordService> {
        val keyPair = Ed25519Keys.generate()
        val recordService = RecordService(
            RecordRepositoryImpl(dir.resolve("ledger.jsonl").toString()),
            CanonicalJsonHasher(),
            Ed25519Signer(keyPair.private),
            Clock.system(ZoneId.of("Asia/Seoul")),
        )
        val guard = PaymentGuardService(paymentAllowed = paymentAllowed, maxAmount = 10000, allowedMerchants = listOf("마트A"))
        val executor = AgentToolExecutor(FakeProductRepository(), guard, recordService)
        return Pair(executor, recordService)
    }

    private val agent = AgentInfo(provider = "anthropic", model = "claude-opus-5", requestId = "msg_01", sessionId = "sess_01")

    @Test
    fun `검색은 저장소 결과를 그대로 돌려준다`(@TempDir dir: Path) {
        val (executor, _) = executor(dir, paymentAllowed = false)
        assertEquals(listOf(Product("계란 30구", 5980, "마트A")), executor.searchProduct("계란"))
        assertEquals(emptyList(), executor.searchProduct("없는거"))
    }

    @Test
    fun `권한 없는 결제 시도는 BLOCKED로 판정되고 장부에 그대로 남는다`(@TempDir dir: Path) {
        val (executor, recordService) = executor(dir, paymentAllowed = false)

        val result = executor.pay(agent, item = "계란 30구", amount = 5980, merchant = "마트A", rawRequest = """{"item":"계란 30구","amount":5980,"merchant":"마트A"}""")

        assertEquals(PayResult(Decision.BLOCKED, "NO_PAYMENT_PERMISSION", recordSeq = 1), result)
        val records = recordService.findAll()
        assertEquals(1, records.size)
        val record = records[0]
        assertEquals(Decision.BLOCKED, record.decision)
        assertEquals("NO_PAYMENT_PERMISSION", record.reason)
        assertEquals(agent, record.agent)
        assertEquals("pay", record.attempt.tool)
        assertEquals("계란 30구", record.attempt.item)
        assertEquals(5980L, record.attempt.amount)
        assertEquals("KRW", record.attempt.currency)
        assertEquals("마트A", record.attempt.merchant)
        assertEquals("""{"item":"계란 30구","amount":5980,"merchant":"마트A"}""", record.rawRequest)
    }

    @Test
    fun `허용된 결제도 장부에 ALLOWED로 남는다`(@TempDir dir: Path) {
        val (executor, recordService) = executor(dir, paymentAllowed = true)

        val result = executor.pay(agent, item = "계란 30구", amount = 5980, merchant = "마트A", rawRequest = "{}")

        assertEquals(PayResult(Decision.ALLOWED, "WITHIN_POLICY", recordSeq = 1), result)
        assertEquals(Decision.ALLOWED, recordService.findAll()[0].decision)
    }

    @Test
    fun `결제를 두 번 시도하면 순번이 1, 2로 이어진다`(@TempDir dir: Path) {
        val (executor, _) = executor(dir, paymentAllowed = false)
        val first = executor.pay(agent, "계란 30구", 5980, "마트A", "{}")
        val second = executor.pay(agent, "우유 1L", 3200, "마트A", "{}")
        assertEquals(1L, first.recordSeq)
        assertEquals(2L, second.recordSeq)
    }
}
