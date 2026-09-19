package pod.app.application.dto

import pod.app.domain.record.AgentInfo
import pod.app.domain.record.Attempt
import pod.app.domain.record.Decision
import pod.app.domain.record.Record

data class RecordDto(
    val seq: Long,
    val prevHash: String,
    val at: String,
    val agent: AgentInfo,
    val attempt: Attempt,
    val decision: Decision,
    val reason: String,
    val rawRequest: String,
    val hash: String,
    val signature: String,
) {
    companion object {
        fun from(record: Record): RecordDto {
            return RecordDto(
                seq = record.seq,
                prevHash = record.prevHash,
                at = record.at,
                agent = record.agent,
                attempt = record.attempt,
                decision = record.decision,
                reason = record.reason,
                rawRequest = record.rawRequest,
                hash = record.hash,
                signature = record.signature,
            )
        }
    }
}
