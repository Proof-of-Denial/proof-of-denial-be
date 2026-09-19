package pod.app.interfaces.agent.res

import pod.app.application.dto.AgentStepDto
import pod.app.domain.agent.AgentStepKind

data class ChatRes(
    val steps: List<AgentStepRes>,
) {
    companion object {
        fun from(dtos: List<AgentStepDto>): ChatRes {
            val steps = ArrayList<AgentStepRes>()
            for (dto in dtos) {
                steps.add(AgentStepRes(kind = dto.kind, content = dto.content))
            }
            return ChatRes(steps = steps)
        }
    }

    data class AgentStepRes(
        val kind: AgentStepKind,
        val content: String,
    )
}
