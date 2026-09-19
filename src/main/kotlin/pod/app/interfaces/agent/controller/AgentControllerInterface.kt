package pod.app.interfaces.agent.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import pod.app.interfaces.agent.req.ChatReq
import pod.app.interfaces.agent.res.ChatRes
import pod.app.interfaces.common.CommonRes

/** 에이전트 API 명세. */
interface AgentControllerInterface {

    /** 사용자 메시지 하나로 에이전트 한 판을 돌리고 대화 전체를 돌려준다. */
    @PostMapping("/chat")
    fun chat(@Valid @RequestBody req: ChatReq): CommonRes<ChatRes>
}
