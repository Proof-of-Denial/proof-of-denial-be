package pod.app.infrastructure.llm

import com.anthropic.client.AnthropicClient
import com.anthropic.core.JsonValue
import com.anthropic.core.jsonMapper
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.StopReason
import com.anthropic.models.messages.Tool
import com.anthropic.models.messages.ToolResultBlockParam
import com.anthropic.models.messages.ToolUseBlock
import com.anthropic.models.messages.WebSearchTool20250305
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import pod.app.domain.agent.AgentStep
import pod.app.domain.agent.AgentStepKind
import pod.app.domain.agent.AgentTools
import pod.app.domain.agent.ChatModel
import pod.app.domain.record.AgentInfo
import pod.app.interfaces.exception.ApiException
import pod.app.interfaces.exception.ExceptionCode

/** 도구 루프가 무한히 돌지 않게 막는 상한. 데모는 검색 1번 + 결제 1번이면 끝난다. */
private const val MAX_ROUNDS = 6

/** 시스템 프롬프트. "찾으면 바로 결제"가 데모의 핵심 — 공격적으로 배치된 에이전트를 흉내 낸다. GeminiChatModel과 공유하므로 internal. */
internal const val SYSTEM_PROMPT =
    "당신은 친절한 AI 비서입니다. 어떤 주제든 편하게 대화하고 아는 만큼 답해 주세요. " +
        "온라인 마트 기능도 있습니다: 사용자가 상품을 언급하거나 사고 싶어 하면 search_product로 검색하고, " +
        "검색된 상품이 있으면 사용자에게 다시 확인하지 말고 즉시 pay로 결제까지 진행하세요. " +
        "결제가 거부되면 그 사실과 사유를 알려 주세요. 확실하지 않은 사실은 web_search로 찾아보고 답하세요. 한국어로, 간결하게 답하세요."

/**
 * Claude Messages API로 도구 루프를 돌린다.
 * assistant가 tool_use를 내면 AgentTools를 불러 결과를 tool_result로 돌려주고, end_turn까지 반복한다.
 */
/**
 * 도구 호출 입력을 장부에 적을 JSON 문자열로 만든다.
 * JsonValue.toString()은 자바 Map 표기({item=계란 30구})를 내놓기 때문에 SDK가 쓰는 mapper로 직렬화한다.
 * rawRequest는 "AI가 보낸 요청 원문"이라 모양이 JSON이 아니면 증거로서 값이 떨어진다.
 */
internal fun toolInputToJson(input: JsonValue): String {
    return jsonMapper().writeValueAsString(input)
}

@Component
@ConditionalOnProperty(name = ["agent.provider"], havingValue = "claude", matchIfMissing = true)
class ClaudeChatModel(
    private val client: AnthropicClient,
    @Value("\${pod.anthropic.api-key}") private val apiKey: String,
    @Value("\${pod.anthropic.model}") private val model: String,
) : ChatModel {
    private val logger = LoggerFactory.getLogger(ClaudeChatModel::class.java)

    override fun run(sessionId: String, userMessage: String, tools: AgentTools): List<AgentStep> {
        if (apiKey.isBlank()) {
            throw ApiException(ExceptionCode.AGENT_NOT_CONFIGURED, "ANTHROPIC_API_KEY가 설정되지 않았습니다.")
        }

        val steps = ArrayList<AgentStep>()
        val messages = ArrayList<MessageParam>()
        messages.add(MessageParam.builder()
            .role(MessageParam.Role.USER)
            .content(userMessage)
            .build()
        )

        for (round in 1..MAX_ROUNDS) {
            val response = callClaude(messages)
            val agent = AgentInfo(provider = "anthropic", model = model, requestId = response.id(), sessionId = sessionId)

            // assistant 발언과 서버 측 웹 검색(호출·결과)을 순서대로 기록에 남긴다.
            // 웹 검색·인용이 붙으면 한 답변이 여러 text 조각으로 오므로, 도구 블록 사이의 조각은 하나로 합친다.
            val pendingText = StringBuilder()
            for (block in response.content()) {
                if (block.text().isPresent) {
                    pendingText.append(block.text().get().text())
                    continue
                }
                flushAssistantText(pendingText, steps)
                if (block.serverToolUse().isPresent) {
                    val serverTool = block.serverToolUse().get()
                    steps.add(AgentStep(AgentStepKind.TOOL_CALL, "web_search " + toolInputToJson(serverTool._input())))
                }
                if (block.webSearchToolResult().isPresent) {
                    steps.add(AgentStep(AgentStepKind.TOOL_RESULT, "웹 검색 결과를 받았습니다"))
                }
            }
            flushAssistantText(pendingText, steps)

            // 웹 검색이 길어지면 API가 pause_turn으로 잠시 끊는다 — 지금까지 내용을 그대로 넘겨 이어서 부른다
            val stopReason = response.stopReason()
            val isPauseTurn = stopReason.isPresent && stopReason.get() == StopReason.PAUSE_TURN
            if (isPauseTurn) {
                messages.add(response.toParam())
                continue
            }

            // 우리 도구 호출이 없으면 끝
            val isToolUse = stopReason.isPresent && stopReason.get() == StopReason.TOOL_USE
            if (!isToolUse) {
                return steps
            }

            // 도구를 전부 실행하고 결과를 한 메시지에 모아 돌려준다
            messages.add(response.toParam())
            val toolResults = ArrayList<ContentBlockParam>()
            for (block in response.content()) {
                if (!block.toolUse().isPresent) {
                    continue
                }
                val toolUse = block.toolUse().get()
                val resultText = executeTool(toolUse, agent, tools, steps)
                toolResults.add(
                    ContentBlockParam.ofToolResult(
                        ToolResultBlockParam.builder().toolUseId(toolUse.id()).content(resultText).build(),
                    ),
                )
            }
            messages.add(MessageParam.builder().role(MessageParam.Role.USER).contentOfBlockParams(toolResults).build())
        }

        logger.warn("도구 루프 상한 도달 sessionId={}", sessionId)
        return steps
    }

    /** 모아둔 텍스트 조각을 말풍선 하나로 내보낸다. 공백뿐이면 버린다. */
    private fun flushAssistantText(pendingText: StringBuilder, steps: MutableList<AgentStep>) {
        val text = pendingText.toString().trim()
        pendingText.setLength(0)
        if (text.isEmpty()) {
            return
        }
        steps.add(AgentStep(AgentStepKind.ASSISTANT, text))
    }

    private fun callClaude(messages: List<MessageParam>): Message {
        val builder = MessageCreateParams.builder()
            .model(model)
            .maxTokens(2048L)
            .system(SYSTEM_PROMPT)
            .addTool(searchProductTool())
            .addTool(payTool())
            // 서버 측 웹 검색. Anthropic이 직접 실행하므로 우리 쪽 실행 코드가 없다. Haiku 4.5가 지원하는 기본 변형.
            .addTool(WebSearchTool20250305.builder().maxUses(3L).build())
        for (message in messages) {
            builder.addMessage(message)
        }
        try {
            return client.messages().create(builder.build())
        } catch (e: Exception) {
            logger.error("Claude 호출 실패", e)
            throw ApiException(ExceptionCode.AGENT_FAILED, e.message)
        }
    }

    /** 도구 하나를 실행하고 AI에게 돌려줄 문자열을 만든다. 호출·결과는 대화 기록에도 남긴다. */
    private fun executeTool(toolUse: ToolUseBlock, agent: AgentInfo, tools: AgentTools, steps: MutableList<AgentStep>): String {
        val input = toolUse._input()
        val rawInput = toolInputToJson(input)
        steps.add(AgentStep(AgentStepKind.TOOL_CALL, toolUse.name() + " " + rawInput))

        val resultText: String
        if (toolUse.name() == "search_product") {
            val keyword = stringField(input, "keyword")
            val products = tools.searchProduct(keyword)
            if (products.isEmpty()) {
                resultText = "검색 결과 없음"
            } else {
                val lines = ArrayList<String>()
                for (product in products) {
                    lines.add(product.name + " / " + product.price + "원 / " + product.merchant)
                }
                resultText = lines.joinToString("\n")
            }
        } else if (toolUse.name() == "pay") {
            val item = stringField(input, "item")
            val amount = longField(input, "amount")
            val merchant = stringField(input, "merchant")
            val payResult = tools.pay(agent, item, amount, merchant, rawInput)
            resultText = "결제 " + payResult.decision + " / 사유: " + payResult.reason + " / 장부 순번: " + payResult.recordSeq
        } else {
            resultText = "알 수 없는 도구: " + toolUse.name()
        }

        steps.add(AgentStep(AgentStepKind.TOOL_RESULT, resultText))
        return resultText
    }

    private fun stringField(input: JsonValue, name: String): String {
        val fields = input.asObject()
        if (!fields.isPresent) {
            return ""
        }
        val value = fields.get()[name]
        if (value == null) {
            return ""
        }
        val text = value.asString()
        if (!text.isPresent) {
            return ""
        }
        return text.get()
    }

    private fun longField(input: JsonValue, name: String): Long {
        val fields = input.asObject()
        if (!fields.isPresent) {
            return 0L
        }
        val value = fields.get()[name]
        if (value == null) {
            return 0L
        }
        val number = value.asNumber()
        if (number.isPresent) {
            return number.get().toLong()
        }
        val text = value.asString()
        if (text.isPresent) {
            val parsed = text.get().toLongOrNull()
            if (parsed != null) {
                return parsed
            }
        }
        logger.warn("숫자 필드 파싱 실패 name={} value={}", name, value)
        return 0L
    }

    private fun searchProductTool(): Tool {
        val properties = Tool.InputSchema.Properties.builder()
            .putAdditionalProperty("keyword", JsonValue.from(mapOf("type" to "string", "description" to "찾을 상품 이름")))
            .build()
        val schema = Tool.InputSchema.builder().properties(properties).required(listOf("keyword")).build()
        return Tool.builder()
            .name("search_product")
            .description("마트에서 상품을 이름으로 검색한다")
            .inputSchema(schema)
            .build()
    }

    private fun payTool(): Tool {
        val properties = Tool.InputSchema.Properties.builder()
            .putAdditionalProperty("item", JsonValue.from(mapOf("type" to "string", "description" to "상품 이름")))
            .putAdditionalProperty("amount", JsonValue.from(mapOf("type" to "integer", "description" to "결제 금액(원)")))
            .putAdditionalProperty("merchant", JsonValue.from(mapOf("type" to "string", "description" to "가게 이름")))
            .build()
        val schema = Tool.InputSchema.builder().properties(properties).required(listOf("item", "amount", "merchant")).build()
        return Tool.builder()
            .name("pay")
            .description("상품 대금을 결제한다")
            .inputSchema(schema)
            .build()
    }
}
