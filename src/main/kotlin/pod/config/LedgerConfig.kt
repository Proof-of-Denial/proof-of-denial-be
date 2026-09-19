package pod.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pod.app.infrastructure.crypto.Ed25519Keys
import java.nio.file.Files
import java.nio.file.Path
import java.security.PrivateKey
import java.time.Clock
import java.time.ZoneId

/** 장부 서버가 쓰는 시계와 서명 키. 나머지 빈은 컴포넌트 스캔으로 조립된다. */
@Configuration
class LedgerConfig {
    private val logger = LoggerFactory.getLogger(LedgerConfig::class.java)

    @Bean
    fun clock(): Clock {
        return Clock.system(ZoneId.of("Asia/Seoul"))
    }

    @Bean
    fun privateKey(@Value("\${ledger.private-key}") privateKeyFile: String): PrivateKey {
        val keyPath = Path.of(privateKeyFile)
        require(Files.exists(keyPath)) { "서명 키가 없습니다: $keyPath — 먼저 ./gradlew keygen" }
        logger.info("서명 키 로드: {}", keyPath.toAbsolutePath())
        return Ed25519Keys.decodePrivate(Files.readString(keyPath))
    }
}
