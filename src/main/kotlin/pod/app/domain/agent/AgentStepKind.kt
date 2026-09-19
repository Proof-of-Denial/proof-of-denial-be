package pod.app.domain.agent

/** 대화 기록 한 줄의 종류. 데모 화면에서 색을 다르게 칠할 때 쓴다. */
enum class AgentStepKind {
    ASSISTANT,
    TOOL_CALL,
    TOOL_RESULT,
}
