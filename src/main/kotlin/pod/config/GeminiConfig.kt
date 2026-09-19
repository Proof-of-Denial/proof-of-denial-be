package pod.config

import com.google.genai.Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Gemini 클라이언트. 키가 비어 있어도 서버는 떠야 하므로(장부 API는 키가 필요 없다) 자리표시 키로 만든다. */
@Configuration
@ConditionalOnProperty(name = ["agent.provider"], havingValue = "gemini")
class GeminiConfig {

    @Bean
    fun geminiClient(@Value("\${gemini.api-key}") apiKey: String): Client {
        var keyToUse = apiKey
        if (apiKey.isBlank()) {
            keyToUse = "not-configured"
        }
        return Client.builder().apiKey(keyToUse).build()
    }
}
