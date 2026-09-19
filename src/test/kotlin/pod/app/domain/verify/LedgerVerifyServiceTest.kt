package pod.app.domain.verify

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import pod.app.domain.record.AgentInfo
import pod.app.domain.record.Attempt
import pod.app.domain.record.Decision
import pod.app.domain.record.Record
import pod.app.domain.record.RecordService
import pod.app.infrastructure.crypto.CanonicalJsonHasher
import pod.app.infrastructure.crypto.Ed25519Keys
import pod.app.infrastructure.crypto.Ed25519SignatureVerifier
import pod.app.infrastructure.crypto.Ed25519Signer
import pod.app.infrastructure.repository.record.persistence.RecordRepositoryImpl
import java.nio.file.Path
import java.security.KeyPair
import java.time.Clock
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LedgerVerifyServiceTest {
    private val hasher = CanonicalJsonHasher()
    private val agent = AgentInfo("anthropic", "claude", "req_1", "s1")

    private fun verifier(keyPair: KeyPair) = LedgerVerifyService(hasher, Ed25519SignatureVerifier(keyPair.public))

    /** 정상 장부 3건 + 키쌍 */
    private fun chain(dir: Path): Pair<List<Record>, KeyPair> {
        val keyPair = Ed25519Keys.generate()
        val service = RecordService(
            RecordRepositoryImpl(dir.resolve("ledger.jsonl").toString()),
            hasher,
            Ed25519Signer(keyPair.private),
            Clock.system(ZoneId.of("Asia/Seoul")),
        )
        service.append(agent, Attempt("pay", "우유", 3200, "KRW", "마트A"), Decision.BLOCKED, "NO_PAYMENT_PERMISSION", "{}")
        service.append(agent, Attempt("pay", "계란 30구", 5980, "KRW", "마트A"), Decision.BLOCKED, "NO_PAYMENT_PERMISSION", "{}")
        service.append(agent, Attempt("pay", "빵", 2500, "KRW", "마트A"), Decision.BLOCKED, "NO_PAYMENT_PERMISSION", "{}")
        return service.findAll() to keyPair
    }

    @Test
    fun `정상 장부는 ok`(@TempDir dir: Path) {
        val (records, keyPair) = chain(dir)
        val result = verifier(keyPair).verify(records)
        assertTrue(result.ok)
        assertEquals(3, result.count)
        assertEquals(records[2].hash, result.head)
        assertEquals(emptyList(), result.problems)
    }

    @Test
    fun `내용만 고치면 그 기록의 HASH_MISMATCH 하나`(@TempDir dir: Path) {
        val (records, keyPair) = chain(dir)
        val tampered = records.toMutableList()
        tampered[1] = tampered[1].copy(attempt = tampered[1].attempt.copy(amount = 980))
        val result = verifier(keyPair).verify(tampered)
        assertFalse(result.ok)
        assertEquals(listOf(ProblemKind.HASH_MISMATCH), result.problems.map { it.kind })
        assertEquals(2L, result.problems[0].seq)
    }

    @Test
    fun `한 줄을 지우면 다음 기록에서 SEQ_GAP과 PREV_MISMATCH`(@TempDir dir: Path) {
        val (records, keyPair) = chain(dir)
        val removed = records.filter { it.seq != 2L }
        val result = verifier(keyPair).verify(removed)
        assertFalse(result.ok)
        assertEquals(listOf(ProblemKind.SEQ_GAP, ProblemKind.PREV_MISMATCH), result.problems.map { it.kind })
        assertEquals(listOf(3L, 3L), result.problems.map { it.seq })
        assertEquals("2번 없음", result.problems[0].detail)
    }

    @Test
    fun `두 줄을 연달아 지우면 범위로 알려준다`(@TempDir dir: Path) {
        val (records, keyPair) = chain(dir)
        val onlyLast = records.filter { it.seq == 3L }
        val result = verifier(keyPair).verify(onlyLast)
        assertEquals("1번부터 2번까지 없음", result.problems[0].detail)
    }

    @Test
    fun `키를 가진 내부자가 고치고 다시 해시·서명해도 다음 기록의 PREV_MISMATCH`(@TempDir dir: Path) {
        val (records, keyPair) = chain(dir)
        val edited = records[1].copy(attempt = records[1].attempt.copy(amount = 980))
        val rehashed = edited.copy(hash = hasher.hashOf(edited.hashBody()))
        val resigned = rehashed.copy(signature = Ed25519Signer(keyPair.private).sign(rehashed.hash))
        val tampered = records.toMutableList()
        tampered[1] = resigned
        val result = verifier(keyPair).verify(tampered)
        assertFalse(result.ok)
        assertEquals(listOf(ProblemKind.PREV_MISMATCH), result.problems.map { it.kind })
        assertEquals(3L, result.problems[0].seq)
    }

    @Test
    fun `끝까지 다 고쳐도 expectHead와 다르면 HEAD_MISMATCH`(@TempDir dir: Path) {
        val (records, keyPair) = chain(dir)
        val result = verifier(keyPair).verify(records, expectHead = "f".repeat(64))
        assertFalse(result.ok)
        assertEquals(listOf(ProblemKind.HEAD_MISMATCH), result.problems.map { it.kind })
        assertNull(result.problems[0].seq)
    }

    @Test
    fun `expectHead가 맞으면 ok`(@TempDir dir: Path) {
        val (records, keyPair) = chain(dir)
        assertTrue(verifier(keyPair).verify(records, expectHead = records[2].hash).ok)
    }

    @Test
    fun `다른 공개키로 검증하면 전부 BAD_SIGNATURE`(@TempDir dir: Path) {
        val (records, _) = chain(dir)
        val other = Ed25519Keys.generate()
        val result = verifier(other).verify(records)
        assertEquals(List(3) { ProblemKind.BAD_SIGNATURE }, result.problems.map { it.kind })
        assertEquals(listOf(1L, 2L, 3L), result.problems.map { it.seq })
    }

    @Test
    fun `빈 장부는 ok이고 head는 null`() {
        val result = verifier(Ed25519Keys.generate()).verify(emptyList())
        assertTrue(result.ok)
        assertEquals(0, result.count)
        assertNull(result.head)
    }
}
