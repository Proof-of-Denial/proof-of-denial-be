package pod.app.domain.record

/** 어느 AI가 보냈나. requestId는 AI 회사가 자기 로그와 대조할 수 있는 열쇠. */
data class AgentInfo(
    val provider: String,
    val model: String,
    val requestId: String,
    val sessionId: String,
)
