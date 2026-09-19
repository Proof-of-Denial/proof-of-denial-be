package pod.app.interfaces.agent.req

import jakarta.validation.constraints.NotBlank

data class ChatReq(
    @field:NotBlank(message = "message는 필수입니다.")
    val message: String,
)
