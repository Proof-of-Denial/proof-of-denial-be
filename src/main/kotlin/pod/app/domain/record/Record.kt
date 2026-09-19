package pod.app.domain.record

/** 첫 기록의 prevHash. 앞 기록이 없다는 뜻. */
const val GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000"

/** 장부에 저장되는 기록 한 줄. */
data class Record(
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
    fun hashBody(): HashBody {
        return HashBody(seq, prevHash, at, agent, attempt, decision, reason, rawRequest)
    }
}
