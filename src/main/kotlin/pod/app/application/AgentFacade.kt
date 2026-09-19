package pod.app.application

import org.springframework.stereotype.Component
import pod.app.application.dto.AgentStepDto
import pod.app.domain.agent.AgentService

@Component
class AgentFacade(
    private val agentService: AgentService,
) {
    fun chat(message: String): List<AgentStepDto> {
        val steps = agentService.chat(message)
        val dtos = ArrayList<AgentStepDto>()
        for (step in steps) {
            dtos.add(AgentStepDto.from(step))
        }
        return dtos
    }
}
