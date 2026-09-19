package pod.app.interfaces.record.req

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import pod.app.domain.record.AgentInfo
import pod.app.domain.record.Attempt
import pod.app.domain.record.Decision

/** 결제 가드가 차단 1건을 넘길 때의 요청. */
data class AddRecordReq(
    @field:Valid
    @field:NotNull(message = "agent는 필수입니다.")
    val agent: AgentReq,

    @field:Valid
    @field:NotNull(message = "attempt는 필수입니다.")
    val attempt: AttemptReq,

    @field:NotNull(message = "decision은 필수입니다.")
    val decision: Decision,

    @field:NotBlank(message = "reason은 필수입니다.")
    val reason: String,

    @field:NotNull(message = "rawRequest는 필수입니다.")
    val rawRequest: String,
) {
    data class AgentReq(
        @field:NotBlank(message = "provider는 필수입니다.")
        val provider: String,
        @field:NotBlank(message = "model은 필수입니다.")
        val model: String,
        @field:NotBlank(message = "requestId는 필수입니다.")
        val requestId: String,
        @field:NotBlank(message = "sessionId는 필수입니다.")
        val sessionId: String,
    ) {
        fun toDomain(): AgentInfo {
            return AgentInfo(provider = provider, model = model, requestId = requestId, sessionId = sessionId)
        }
    }

    data class AttemptReq(
        @field:NotBlank(message = "tool은 필수입니다.")
        val tool: String,
        @field:NotBlank(message = "item은 필수입니다.")
        val item: String,
        @field:PositiveOrZero(message = "amount는 0 이상이어야 합니다.")
        val amount: Long,
        @field:NotBlank(message = "currency는 필수입니다.")
        val currency: String,
        @field:NotBlank(message = "merchant는 필수입니다.")
        val merchant: String,
    ) {
        fun toDomain(): Attempt {
            return Attempt(tool = tool, item = item, amount = amount, currency = currency, merchant = merchant)
        }
    }
}
