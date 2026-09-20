package pod.config

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.xrpl.xrpl4j.client.XrplClient
import org.xrpl.xrpl4j.client.faucet.FaucetClient
import org.xrpl.xrpl4j.crypto.keys.KeyPair
import org.xrpl.xrpl4j.crypto.keys.Passphrase
import org.xrpl.xrpl4j.crypto.keys.Seed

/**
 * XRPL 테스트넷 접속 빈. 계정은 비밀 문구에서 결정적으로 파생한다 — 매 기동마다 같은 주소가 나온다.
 * Clock 빈은 LedgerConfig에 이미 있으므로 여기서 다시 만들지 않는다.
 */
@Configuration
class XrplConfig {
    private val logger = LoggerFactory.getLogger(XrplConfig::class.java)

    @Bean
    fun xrplClient(@Value("\${xrpl.rpc-url}") rpcUrl: String): XrplClient {
        return XrplClient(rpcUrl.toHttpUrl())
    }

    @Bean
    fun faucetClient(@Value("\${xrpl.faucet-url}") faucetUrl: String): FaucetClient {
        return FaucetClient.construct(faucetUrl.toHttpUrl())
    }

    @Bean
    fun xrplKeyPair(@Value("\${xrpl.passphrase}") passphrase: String): KeyPair {
        require(passphrase.isNotBlank()) { "XRPL_PASSPHRASE must be configured and non-blank" }
        val seed = Seed.ed25519SeedFromPassphrase(Passphrase.of(passphrase))
        val keyPair = seed.deriveKeyPair()
        val address = keyPair.publicKey().deriveAddress()
        logger.info("XRPL 계정 {}", address.value())
        return keyPair
    }
}
