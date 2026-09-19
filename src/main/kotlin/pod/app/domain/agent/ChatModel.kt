package pod.app.domain.agent

/** LLM 포트. 사용자 메시지 하나를 받아 도구 루프를 끝까지 돌리고 대화 기록을 돌려준다. */
interface ChatModel {
    fun run(sessionId: String, userMessage: String, tools: AgentTools): List<AgentStep>
}
