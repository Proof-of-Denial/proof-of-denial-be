package pod.config

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

/** Claude SDK 클라이언트. 키가 비어 있어도 서버는 떠야 하므로(장부 API는 키가 필요 없다) 자리표시 키로 만든다. */
@Configuration
@ConditionalOnProperty(name = ["agent.provider"], havingValue = "claude", matchIfMissing = true)
class ClaudeConfig {
    private val logger = LoggerFactory.getLogger(ClaudeConfig::class.java)

    @Bean
    fun anthropicClient(@Value("\${pod.anthropic.api-key}") apiKey: String): AnthropicClient {
        // 값은 절대 찍지 않는다. 길이와 형식만 — 셸의 다른 토큰이 섞여 들어왔는지 기동 로그에서 바로 보이게.
        val looksLikeApiKey = apiKey.startsWith("sk-ant-api")
        logger.info("Anthropic 키: {}자, 형식 {}", apiKey.length, if (looksLikeApiKey) "sk-ant-api ✓" else "API 키 아님")
        var keyToUse = apiKey
        if (apiKey.isBlank()) {
            keyToUse = "not-configured"
        }
        return AnthropicOkHttpClient.builder()
            .apiKey(keyToUse)
            .build()
    }
}
