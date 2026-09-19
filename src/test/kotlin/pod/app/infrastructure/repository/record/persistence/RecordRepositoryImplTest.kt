package pod.app.infrastructure.repository.record.persistence

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import pod.app.domain.record.AgentInfo
import pod.app.domain.record.Attempt
import pod.app.domain.record.Decision
import pod.app.domain.record.GENESIS_HASH
import pod.app.domain.record.Record
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecordRepositoryImplTest {
    private fun sample(seq: Long, prevHash: String): Record {
        return Record(
            seq = seq, prevHash = prevHash, at = "2026-09-20T14:03:11+09:00",
            agent = AgentInfo("anthropic", "claude", "req_$seq", "s1"),
            attempt = Attempt("pay", "계란 30구", 5980, "KRW", "마트A"),
            decision = Decision.BLOCKED, reason = "NO_PAYMENT_PERMISSION", rawRequest = "{}",
            hash = "ab".repeat(32), signature = "c2ln",
        )
    }

    @Test
    fun `파일이 없으면 빈 리스트`(@TempDir dir: Path) {
        val repository = RecordRepositoryImpl(dir.resolve("none.jsonl").toString())
        assertEquals(emptyList(), repository.findAll())
    }

    @Test
    fun `save 두 번 하면 두 줄, 읽으면 그대로 돌아온다`(@TempDir dir: Path) {
        val file = dir.resolve("sub/ledger.jsonl")
        val repository = RecordRepositoryImpl(file.toString())
        val r1 = sample(1, GENESIS_HASH)
        val r2 = sample(2, r1.hash)
        repository.save(r1)
        repository.save(r2)

        val lines = Files.readAllLines(file)
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains(""""amount":5980"""), "데모의 sed 패턴이 맞도록 amount가 그대로 보여야 한다")
        assertTrue(lines[0].contains(""""decision":"BLOCKED""""))
        assertEquals(listOf(r1, r2), repository.findAll())
    }
}
