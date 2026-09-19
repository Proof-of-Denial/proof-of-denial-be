package pod.app.domain.agent

/** 에이전트 대화 기록 한 줄. AI 발언, 도구 호출, 도구 결과 중 하나. */
data class AgentStep(
    val kind: AgentStepKind,
    val content: String,
)
