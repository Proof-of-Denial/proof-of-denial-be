package pod.app.domain.anchor

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import pod.app.domain.record.AgentInfo
import pod.app.domain.record.Attempt
import pod.app.domain.record.Decision
import pod.app.domain.record.RecordService
import pod.app.infrastructure.crypto.CanonicalJsonHasher
import pod.app.infrastructure.crypto.Ed25519Keys
import pod.app.infrastructure.crypto.Ed25519Signer
import pod.app.infrastructure.repository.anchor.persistence.AnchorReceiptRepositoryImpl
import pod.app.infrastructure.repository.record.persistence.RecordRepositoryImpl
import pod.app.interfaces.exception.ApiException
import pod.app.interfaces.exception.ExceptionCode
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AnchorServiceTest {

    private val fixedClock = Clock.fixed(Instant.parse("2026-09-20T05:03:11Z"), ZoneId.of("Asia/Seoul"))
    private val agent = AgentInfo("anthropic", "claude", "req_1", "s1")

    /** 테스트용 체인. writeHead는 seq로 가짜 txHash를 만들고, readHead는 그때 저장해둔 지문을 돌려준다. */
    private class FakeChainAnchor : ChainAnchor {
        private val written = HashMap<String, String>()

        override fun writeHead(seq: Long, hash: String): ChainWriteResult {
            val txHash = "TX" + seq
            written[txHash] = hash
            return ChainWriteResult(txHash, "rADDR")
        }

        override fun readHead(txHash: String): String? {
            return written[txHash]
        }
    }

    private fun service(dir: Path, chainAnchor: ChainAnchor): Pair<AnchorService, RecordService> {
        val keyPair = Ed25519Keys.generate()
        val recordService = RecordService(
            RecordRepositoryImpl(dir.resolve("ledger.jsonl").toString()),
            CanonicalJsonHasher(),
            Ed25519Signer(keyPair.private),
            fixedClock,
        )
        val anchorService = AnchorService(
            recordService,
            chainAnchor,
            AnchorReceiptRepositoryImpl(dir.resolve("anchors.jsonl").toString()),
            fixedClock,
        )
        return Pair(anchorService, recordService)
    }

    @Test
    fun `기록 2건을 넣고 도장을 찍으면 마지막 지문이 영수증으로 남는다`(@TempDir dir: Path) {
        val (anchorService, recordService) = service(dir, FakeChainAnchor())
        recordService.append(agent, Attempt("pay", "우유", 3200, "KRW", "마트A"), Decision.BLOCKED, "NO_PAYMENT_PERMISSION", "{}")
        val second = recordService.append(agent, Attempt("pay", "계란 30구", 5980, "KRW", "마트A"), Decision.BLOCKED, "NO_PAYMENT_PERMISSION", "{}")

        val receipt = anchorService.anchorNow()

        val expected = AnchorReceipt(2, second.hash, "TX2", "rADDR", "2026-09-20T14:03:11+09:00")
        assertEquals(expected, receipt)
        assertEquals(expected, anchorService.latest())
    }

    @Test
    fun `장부가 비어 있으면 ApiException LEDGER_EMPTY`(@TempDir dir: Path) {
        val (anchorService, _) = service(dir, FakeChainAnchor())
        val exception = assertFailsWith<ApiException> { anchorService.anchorNow() }
        assertEquals(ExceptionCode.LEDGER_EMPTY, exception.exceptionCode)
    }

    @Test
    fun `두 번 찍으면 영수증이 2건 쌓이고 latest는 두 번째`(@TempDir dir: Path) {
        val (anchorService, recordService) = service(dir, FakeChainAnchor())
        recordService.append(agent, Attempt("pay", "우유", 3200, "KRW", "마트A"), Decision.BLOCKED, "NO_PAYMENT_PERMISSION", "{}")
        val first = anchorService.anchorNow()
        recordService.append(agent, Attempt("pay", "빵", 2500, "KRW", "마트A"), Decision.BLOCKED, "NO_PAYMENT_PERMISSION", "{}")
        val second = anchorService.anchorNow()

        val repository = AnchorReceiptRepositoryImpl(dir.resolve("anchors.jsonl").toString())
        assertEquals(listOf(first, second), repository.findAll())
        assertEquals(second, anchorService.latest())
    }
}
