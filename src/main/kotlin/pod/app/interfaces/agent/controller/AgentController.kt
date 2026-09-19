package pod.app.interfaces.agent.controller

import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pod.app.application.AgentFacade
import pod.app.interfaces.agent.req.ChatReq
import pod.app.interfaces.agent.res.ChatRes
import pod.app.interfaces.common.CommonRes

@RestController
@RequestMapping("/api/v1/agent")
class AgentController(
    private val agentFacade: AgentFacade,
) : AgentControllerInterface {

    override fun chat(req: ChatReq): CommonRes<ChatRes> {
        val dtos = agentFacade.chat(req.message)
        return CommonRes.success(ChatRes.from(dtos))
    }
}
