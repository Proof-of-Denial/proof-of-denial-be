package pod.config

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Claude SDK 클라이언트. 키가 비어 있어도 서버는 떠야 하므로(장부 API는 키가 필요 없다) 자리표시 키로 만든다. */
@Configuration
class ClaudeConfig {

    @Bean
    fun anthropicClient(@Value("\${anthropic.api-key}") apiKey: String): AnthropicClient {
        var keyToUse = apiKey
        if (apiKey.isBlank()) {
            keyToUse = "not-configured"
        }
        return AnthropicOkHttpClient.builder()
            .apiKey(keyToUse)
            .build()
    }
}
