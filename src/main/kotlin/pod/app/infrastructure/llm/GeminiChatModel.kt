package pod.app.infrastructure.llm

import com.google.genai.Client
import com.google.genai.types.Content
import com.google.genai.types.FunctionCall
import com.google.genai.types.FunctionDeclaration
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.GenerateContentResponse
import com.google.genai.types.Part
import com.google.genai.types.Schema
import com.google.genai.types.Tool
import com.google.genai.types.Type
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
import java.util.UUID

/** 도구 루프가 무한히 돌지 않게 막는 상한. ClaudeChatModel과 같은 값(6)을 이 파일에도 둔다. */
private const val MAX_ROUNDS = 6

/**
 * Gemini generateContent API로 도구 루프를 돌린다.
 * 모델이 함수 호출을 내면 AgentTools를 불러 결과를 function response로 돌려주고, 함수 호출이 없을 때까지 반복한다.
 * 도구 루프의 모양은 ClaudeChatModel과 같고 SDK 타입만 다르다.
 */
@Component
@ConditionalOnProperty(name = ["agent.provider"], havingValue = "gemini")
class GeminiChatModel(
    private val client: Client,
    @Value("\${gemini.api-key}") private val apiKey: String,
    @Value("\${gemini.model}") private val model: String,
) : ChatModel {
    private val logger = LoggerFactory.getLogger(GeminiChatModel::class.java)

    override fun run(sessionId: String, userMessage: String, tools: AgentTools): List<AgentStep> {
        if (apiKey.isBlank()) {
            throw ApiException(ExceptionCode.AGENT_NOT_CONFIGURED, "GEMINI_API_KEY가 설정되지 않았습니다.")
        }

        val steps = ArrayList<AgentStep>()
        val history = ArrayList<Content>()
        history.add(
            Content.builder()
                .role("user")
                .parts(listOf(Part.fromText(userMessage)))
                .build(),
        )

        for (round in 1..MAX_ROUNDS) {
            val response = callGemini(history)
            val agent = AgentInfo(provider = "google", model = model, requestId = requestIdOf(response), sessionId = sessionId)

            // assistant 발언은 조각을 하나로 합쳐 말풍선 하나로 남긴다. 공백뿐이면 버린다.
            val assistantText = StringBuilder()
            for (part in textPartsOf(response)) {
                if (part.text().isPresent) {
                    assistantText.append(part.text().get())
                }
            }
            val trimmedText = assistantText.toString().trim()
            if (trimmedText.isNotEmpty()) {
                steps.add(AgentStep(AgentStepKind.ASSISTANT, trimmedText))
            }

            // 함수 호출이 없으면 끝
            val functionCalls = functionCallsOf(response)
            if (functionCalls.isEmpty()) {
                return steps
            }

            // 모델 턴을 히스토리에 남기고, 도구를 전부 실행해 결과를 한 Content(role user)에 모아 돌려준다
            history.add(modelContentOf(response))
            val resultParts = ArrayList<Part>()
            for (call in functionCalls) {
                val resultText = executeTool(call, agent, tools, steps)
                val responseMap: Map<String, Any> = mapOf("result" to resultText)
                resultParts.add(Part.fromFunctionResponse(nameOf(call), responseMap))
            }
            history.add(Content.builder().role("user").parts(resultParts).build())
        }

        logger.warn("도구 루프 상한 도달 sessionId={}", sessionId)
        return steps
    }

    private fun callGemini(history: List<Content>): GenerateContentResponse {
        val systemInstruction = Content.builder().parts(listOf(Part.fromText(SYSTEM_PROMPT))).build()
        val config = GenerateContentConfig.builder()
            .systemInstruction(systemInstruction)
            .tools(listOf(toolDeclarations()))
            .build()
        try {
            return client.models.generateContent(model, history, config)
        } catch (e: Exception) {
            logger.error("Gemini 호출 실패", e)
            throw ApiException(ExceptionCode.AGENT_FAILED, e.message)
        }
    }

    /** SDK가 candidate가 없을 때 null을 돌려줄 수 있어(JSR-305 strict) 빈 목록으로 방어한다. */
    private fun textPartsOf(response: GenerateContentResponse): List<Part> {
        val parts = response.parts()
        if (parts == null) {
            return emptyList()
        }
        return parts
    }

    private fun functionCallsOf(response: GenerateContentResponse): List<FunctionCall> {
        val calls = response.functionCalls()
        if (calls == null) {
            return emptyList()
        }
        return calls
    }

    /** 응답의 첫 candidate가 낸 Content(함수 호출 파트 포함)를 그대로 히스토리에 넣을 모델 턴으로 쓴다. */
    private fun modelContentOf(response: GenerateContentResponse): Content {
        val candidates = response.candidates()
        if (!candidates.isPresent || candidates.get().isEmpty()) {
            logger.warn("응답에 candidate가 없다")
            return Content.builder().role("model").parts(emptyList<Part>()).build()
        }
        val content = candidates.get()[0].content()
        if (!content.isPresent) {
            logger.warn("첫 candidate에 content가 없다")
            return Content.builder().role("model").parts(emptyList<Part>()).build()
        }
        return content.get()
    }

    private fun requestIdOf(response: GenerateContentResponse): String {
        if (response.responseId().isPresent) {
            return response.responseId().get()
        }
        return "gemini-" + UUID.randomUUID().toString().substring(0, 8)
    }

    private fun nameOf(call: FunctionCall): String {
        if (call.name().isPresent) {
            return call.name().get()
        }
        return "unknown"
    }

    private fun argsOf(call: FunctionCall): Map<String, Any?> {
        if (call.args().isPresent) {
            return call.args().get()
        }
        return emptyMap()
    }

    /** 함수 호출 하나를 실행하고 AI에게 돌려줄 문자열을 만든다. 호출·결과는 대화 기록에도 남긴다. */
    private fun executeTool(call: FunctionCall, agent: AgentInfo, tools: AgentTools, steps: MutableList<AgentStep>): String {
        val name = nameOf(call)
        val args = argsOf(call)
        val rawInput = toolArgsToJson(args)
        steps.add(AgentStep(AgentStepKind.TOOL_CALL, name + " " + rawInput))

        val resultText: String
        if (name == "search_product") {
            val keyword = stringArg(args, "keyword")
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
        } else if (name == "pay") {
            val item = stringArg(args, "item")
            val amount = longArg(args, "amount")
            val merchant = stringArg(args, "merchant")
            val payResult = tools.pay(agent, item, amount, merchant, rawInput)
            resultText = "결제 " + payResult.decision + " / 사유: " + payResult.reason + " / 장부 순번: " + payResult.recordSeq
        } else {
            resultText = "알 수 없는 도구: " + name
        }

        steps.add(AgentStep(AgentStepKind.TOOL_RESULT, resultText))
        return resultText
    }

    private fun stringArg(args: Map<String, Any?>, name: String): String {
        val value = args[name]
        if (value == null) {
            return ""
        }
        return value.toString()
    }

    private fun longArg(args: Map<String, Any?>, name: String): Long {
        val value = args[name]
        if (value == null) {
            return 0L
        }
        if (value is Number) {
            return value.toLong()
        }
        val parsed = value.toString().toLongOrNull()
        if (parsed != null) {
            return parsed
        }
        logger.warn("숫자 인자 파싱 실패 name={} value={}", name, value)
        return 0L
    }

    private fun toolDeclarations(): Tool {
        return Tool.builder()
            .functionDeclarations(listOf(searchProductDeclaration(), payDeclaration()))
            .build()
    }

    private fun searchProductDeclaration(): FunctionDeclaration {
        val properties = mapOf(
            "keyword" to Schema.builder().type(Type.Known.STRING).description("찾을 상품 이름").build(),
        )
        val schema = Schema.builder().type(Type.Known.OBJECT).properties(properties).required(listOf("keyword")).build()
        return FunctionDeclaration.builder()
            .name("search_product")
            .description("마트에서 상품을 이름으로 검색한다")
            .parameters(schema)
            .build()
    }

    private fun payDeclaration(): FunctionDeclaration {
        val properties = mapOf(
            "item" to Schema.builder().type(Type.Known.STRING).description("상품 이름").build(),
            "amount" to Schema.builder().type(Type.Known.INTEGER).description("결제 금액(원)").build(),
            "merchant" to Schema.builder().type(Type.Known.STRING).description("가게 이름").build(),
        )
        val schema = Schema.builder().type(Type.Known.OBJECT).properties(properties).required(listOf("item", "amount", "merchant")).build()
        return FunctionDeclaration.builder()
            .name("pay")
            .description("상품 대금을 결제한다")
            .parameters(schema)
            .build()
    }
}
