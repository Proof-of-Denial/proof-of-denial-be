package pod.app.domain.agent

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

/** 사용자 메시지 하나로 에이전트 한 판을 돌린다. 세션 id는 한 판마다 새로 만든다. */
@Service
class AgentService(
    private val chatModel: ChatModel,
    private val agentToolExecutor: AgentToolExecutor,
) {
    private val logger = LoggerFactory.getLogger(AgentService::class.java)

    fun chat(userMessage: String): List<AgentStep> {
        val sessionId = UUID.randomUUID().toString()
        logger.info("에이전트 시작 sessionId={} message={}", sessionId, userMessage)

        val steps = chatModel.run(sessionId, userMessage, agentToolExecutor)

        logger.info("에이전트 종료 sessionId={} steps={}", sessionId, steps.size)
        return steps
    }
}
