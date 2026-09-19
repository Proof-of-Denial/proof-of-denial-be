package pod.app.domain.record

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pod.app.domain.crypto.RecordHasher
import pod.app.domain.crypto.RecordSigner
import java.time.Clock
import java.time.OffsetDateTime

/**
 * 차단 1건을 받아 순번 · 앞 지문 · 시각 · 지문 · 서명을 채워 장부에 붙인다.
 * 해커톤 규모라 매번 파일을 다시 읽는다. 동시 요청은 @Synchronized로 직렬화.
 */
@Service
class RecordService(
    private val recordRepository: RecordRepository,
    private val recordHasher: RecordHasher,
    private val recordSigner: RecordSigner,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(RecordService::class.java)

    @Synchronized
    fun append(agent: AgentInfo, attempt: Attempt, decision: Decision, reason: String, rawRequest: String): Record {
        val existing = recordRepository.findAll()
        // 줄이 하나 지워진 채로 다시 붙는 경우가 있어(데모의 변조 시나리오), 개수가 아니라 마지막 seq에서 이어간다.
        val seq = (existing.lastOrNull()?.seq ?: 0L) + 1
        val prevHash = existing.lastOrNull()?.hash ?: GENESIS_HASH
        val at = OffsetDateTime.now(clock).toString()

        val body = HashBody(seq, prevHash, at, agent, attempt, decision, reason, rawRequest)
        val hash = recordHasher.hashOf(body)
        val signature = recordSigner.sign(hash)

        val record = Record(seq, prevHash, at, agent, attempt, decision, reason, rawRequest, hash, signature)
        recordRepository.save(record)
        logger.info("기록 추가 seq={} decision={} item={} amount={} hash={}", seq, decision, attempt.item, attempt.amount, hash.take(12))
        return record
    }

    fun findAll(): List<Record> {
        return recordRepository.findAll()
    }

    fun findBySeq(seq: Long): Record? {
        return recordRepository.findAll().firstOrNull { it.seq == seq }
    }

    fun head(): Head? {
        val last = recordRepository.findAll().lastOrNull() ?: return null
        return Head(last.seq, last.hash)
    }
}
