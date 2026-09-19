package pod.app.domain.record

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import pod.app.infrastructure.crypto.CanonicalJsonHasher
import pod.app.infrastructure.crypto.Ed25519Keys
import pod.app.infrastructure.crypto.Ed25519SignatureVerifier
import pod.app.infrastructure.crypto.Ed25519Signer
import pod.app.infrastructure.repository.record.persistence.RecordRepositoryImpl
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPair
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecordServiceTest {
    private val fixedClock = Clock.fixed(Instant.parse("2026-09-20T05:03:11Z"), ZoneId.of("Asia/Seoul"))
    private val hasher = CanonicalJsonHasher()

    private fun service(dir: Path, keyPair: KeyPair) = RecordService(
        recordRepository = RecordRepositoryImpl(dir.resolve("ledger.jsonl").toString()),
        recordHasher = hasher,
        recordSigner = Ed25519Signer(keyPair.private),
        clock = fixedClock,
    )

    private val agent = AgentInfo("anthropic", "claude", "req_1", "s1")

    @Test
    fun `첫 기록은 입력 그대로에 seq 1, GENESIS, 서울 시각, 지문, 서명이 붙는다`(@TempDir dir: Path) {
        val keyPair = Ed25519Keys.generate()
        val attempt = Attempt("pay", "우유", 3200, "KRW", "마트A")
        val record = service(dir, keyPair).append(agent, attempt, Decision.BLOCKED, "NO_PAYMENT_PERMISSION", """{"tool":"pay"}""")

        val expectedHash = hasher.hashOf(record.hashBody())
        assertEquals(
            Record(
                seq = 1,
                prevHash = GENESIS_HASH,
                at = "2026-09-20T14:03:11+09:00",
                agent = agent,
                attempt = attempt,
                decision = Decision.BLOCKED,
                reason = "NO_PAYMENT_PERMISSION",
                rawRequest = """{"tool":"pay"}""",
                hash = expectedHash,
                signature = record.signature,
            ),
            record,
        )
        assertEquals(64, record.hash.length)
        assertTrue(Ed25519SignatureVerifier(keyPair.public).verify(record.hash, record.signature))
    }

    @Test
    fun `세 번 넣으면 prevHash가 앞 기록의 hash로 이어진다`(@TempDir dir: Path) {
        val keyPair = Ed25519Keys.generate()
        val service = service(dir, keyPair)
        val r1 = service.append(agent, Attempt("pay", "우유", 3200, "KRW", "마트A"), Decision.BLOCKED, "NO_PAYMENT_PERMISSION", "{}")
        val r2 = service.append(agent, Attempt("pay", "계란 30구", 5980, "KRW", "마트A"), Decision.BLOCKED, "NO_PAYMENT_PERMISSION", "{}")
        val r3 = service.append(agent, Attempt("pay", "빵", 2500, "KRW", "마트A"), Decision.BLOCKED, "NO_PAYMENT_PERMISSION", "{}")

        assertEquals(listOf(1L, 2L, 3L), listOf(r1.seq, r2.seq, r3.seq))
        assertEquals(r1.hash, r2.prevHash)
        assertEquals(r2.hash, r3.prevHash)
        assertTrue(r1.hash != r2.hash && r2.hash != r3.hash)
        assertEquals(listOf(r1, r2, r3), service.findAll())
        assertEquals(r2, service.findBySeq(2))
        assertNull(service.findBySeq(99))
        assertEquals(Head(3, r3.hash), service.head())
    }

    @Test
    fun `서비스를 새로 만들어도 파일에서 이어서 붙는다`(@TempDir dir: Path) {
        val keyPair = Ed25519Keys.generate()
        val attempt = Attempt("pay", "우유", 3200, "KRW", "마트A")
        val first = service(dir, keyPair).append(agent, attempt, Decision.BLOCKED, "NO_PAYMENT_PERMISSION", "{}")
        val second = service(dir, keyPair).append(agent, attempt, Decision.BLOCKED, "NO_PAYMENT_PERMISSION", "{}")
        assertEquals(2L, second.seq)
        assertEquals(first.hash, second.prevHash)
    }

    @Test
    fun `줄이 하나 지워진 채로 다시 붙이면 seq는 개수가 아니라 마지막 기록에서 이어간다`(@TempDir dir: Path) {
        val keyPair = Ed25519Keys.generate()
        val attempt = Attempt("pay", "우유", 3200, "KRW", "마트A")
        val ledgerFile = dir.resolve("ledger.jsonl")
        val serviceForSeeding = service(dir, keyPair)
        serviceForSeeding.append(agent, attempt, Decision.BLOCKED, "NO_PAYMENT_PERMISSION", "{}") // seq 1
        serviceForSeeding.append(agent, attempt, Decision.BLOCKED, "NO_PAYMENT_PERMISSION", "{}") // seq 2 (곧 삭제)
        val seq3 = serviceForSeeding.append(agent, attempt, Decision.BLOCKED, "NO_PAYMENT_PERMISSION", "{}") // seq 3

        // 가운데 줄(seq 2)만 파일에서 직접 지운다. sed 대신 표준 파일 API로.
        val linesWithoutSeq2 = Files.readAllLines(ledgerFile).filterNot { it.contains("\"seq\":2,") }
        Files.write(ledgerFile, linesWithoutSeq2)

        val seq4 = service(dir, keyPair).append(agent, attempt, Decision.BLOCKED, "NO_PAYMENT_PERMISSION", "{}")

        assertEquals(4L, seq4.seq)
        assertEquals(seq3.hash, seq4.prevHash)
    }

    @Test
    fun `비어 있으면 head는 null`(@TempDir dir: Path) {
        assertNull(service(dir, Ed25519Keys.generate()).head())
    }

    @Test
    fun `hashBody는 hash·signature만 뺀 나머지를 필드 하나도 안 섞고 옮긴다`() {
        val attempt = Attempt("pay", "우유", 3200, "KRW", "마트A")
        val record = Record(
            seq = 7, prevHash = "a91f", at = "2026-09-20T14:03:11+09:00",
            agent = agent, attempt = attempt, decision = Decision.ALLOWED,
            reason = "WITHIN_LIMIT", rawRequest = "{\"x\":1}", hash = "e7b2", signature = "sig",
        )
        assertEquals(
            HashBody(
                seq = 7, prevHash = "a91f", at = "2026-09-20T14:03:11+09:00",
                agent = agent, attempt = attempt, decision = Decision.ALLOWED,
                reason = "WITHIN_LIMIT", rawRequest = "{\"x\":1}",
            ),
            record.hashBody(),
        )
    }
}
