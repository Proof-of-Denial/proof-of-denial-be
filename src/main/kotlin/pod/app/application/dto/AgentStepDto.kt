package pod.app.application.dto

import pod.app.domain.agent.AgentStep
import pod.app.domain.agent.AgentStepKind

data class AgentStepDto(
    val kind: AgentStepKind,
    val content: String,
) {
    companion object {
        fun from(step: AgentStep): AgentStepDto {
            return AgentStepDto(kind = step.kind, content = step.content)
        }
    }
}
