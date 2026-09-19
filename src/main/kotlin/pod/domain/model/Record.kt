package pod.domain.model

/** 첫 기록의 prevHash. 앞 기록이 없다는 뜻. */
const val GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000"

/** 결제 가드의 판정. */
enum class Decision { BLOCKED, ALLOWED }

/** 어느 AI가 보냈나. requestId는 AI 회사가 자기 로그와 대조할 수 있는 열쇠. */
data class AgentInfo(
    val provider: String,
    val model: String,
    val requestId: String,
    val sessionId: String,
)

/** AI가 무엇을 하려 했나. */
data class Attempt(
    val tool: String,
    val item: String,
    val amount: Long,
    val currency: String,
    val merchant: String,
)

/** 결제 가드가 장부에 넘기는 입력. 나머지 필드는 장부가 채운다. */
data class RecordInput(
    val agent: AgentInfo,
    val attempt: Attempt,
    val decision: Decision,
    val reason: String,
    val rawRequest: String,
)

/** 지문(hash) 계산 대상. hash·signature를 뺀 전부. */
data class HashBody(
    val seq: Long,
    val prevHash: String,
    val at: String,
    val agent: AgentInfo,
    val attempt: Attempt,
    val decision: Decision,
    val reason: String,
    val rawRequest: String,
)

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
    fun hashBody() = HashBody(seq, prevHash, at, agent, attempt, decision, reason, rawRequest)
}

/** 장부 마지막 기록의 순번과 지문. 나중에 블록체인에 올릴 값. */
data class Head(val seq: Long, val hash: String)
