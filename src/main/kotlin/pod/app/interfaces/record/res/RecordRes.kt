package pod.app.interfaces.record.res

import pod.app.application.dto.RecordDto
import pod.app.domain.record.AgentInfo
import pod.app.domain.record.Attempt
import pod.app.domain.record.Decision

data class RecordRes(
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
        fun from(dto: RecordDto): RecordRes {
            return RecordRes(
                seq = dto.seq,
                prevHash = dto.prevHash,
                at = dto.at,
                agent = dto.agent,
                attempt = dto.attempt,
                decision = dto.decision,
                reason = dto.reason,
                rawRequest = dto.rawRequest,
                hash = dto.hash,
                signature = dto.signature,
            )
        }
    }
}
