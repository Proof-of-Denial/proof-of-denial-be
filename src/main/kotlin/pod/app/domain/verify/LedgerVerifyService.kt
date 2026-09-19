package pod.app.domain.verify

import pod.app.domain.crypto.RecordHasher
import pod.app.domain.crypto.SignatureVerifier
import pod.app.domain.record.GENESIS_HASH
import pod.app.domain.record.Record

/**
 * 장부 전체를 앞에서부터 훑으며 고침·삭제·위조를 잡는다.
 * 검증 CLI가 Spring 없이 직접 만들어 쓰므로 @Service를 붙이지 않고, 저장소 대신 기록 목록을 직접 받는다.
 */
class LedgerVerifyService(
    private val recordHasher: RecordHasher,
    private val signatureVerifier: SignatureVerifier,
) {
    fun verify(records: List<Record>, expectHead: String? = null): VerifyResult {
        val problems = mutableListOf<Problem>()
        var expectedSeq = 1L
        var prevHash = GENESIS_HASH

        for (record in records) {
            if (record.seq != expectedSeq) {
                val detail = if (record.seq > expectedSeq) {
                    val missingCount = record.seq - expectedSeq
                    if (missingCount == 1L) "${expectedSeq}번 없음" else "${expectedSeq}번부터 ${record.seq - 1}번까지 없음"
                } else {
                    "순번 ${record.seq}가 다시 나옴 (${expectedSeq}번이어야 함)"
                }
                problems.add(Problem(record.seq, ProblemKind.SEQ_GAP, detail))
            }

            val recomputedHash = recordHasher.hashOf(record.hashBody())
            if (recomputedHash != record.hash) {
                problems.add(Problem(record.seq, ProblemKind.HASH_MISMATCH, "내용이 지문과 다름 — 고쳐졌음"))
            }

            if (record.prevHash != prevHash) {
                problems.add(Problem(record.seq, ProblemKind.PREV_MISMATCH, "앞 지문이 ${prevHash.take(8)}…이어야 하는데 ${record.prevHash.take(8)}…"))
            }

            if (!signatureVerifier.verify(record.hash, record.signature)) {
                problems.add(Problem(record.seq, ProblemKind.BAD_SIGNATURE, "서명이 맞지 않음"))
            }

            expectedSeq = record.seq + 1
            prevHash = record.hash
        }

        val head = records.lastOrNull()?.hash
        if (expectHead != null && head != expectHead) {
            problems.add(Problem(null, ProblemKind.HEAD_MISMATCH, "마지막 지문이 블록체인에 적힌 값과 다름"))
        }

        return VerifyResult(ok = problems.isEmpty(), count = records.size, head = head, problems = problems)
    }
}
