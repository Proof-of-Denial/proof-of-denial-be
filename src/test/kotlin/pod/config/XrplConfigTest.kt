package pod.config

import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class XrplConfigTest {
    @Test
    fun `reject missing or blank passphrases before deriving an account`() {
        val config = XrplConfig()
        for (passphrase in listOf("", " ", "\t\n")) {
            assertFailsWith<IllegalArgumentException> {
                config.xrplKeyPair(passphrase)
            }
        }
    }
}
