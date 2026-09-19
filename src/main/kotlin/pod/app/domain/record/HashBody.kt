package pod.app.domain.record

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
