package pod.app.infrastructure.crypto

import org.springframework.stereotype.Component
import pod.app.domain.crypto.RecordSigner
import java.security.PrivateKey
import java.security.Signature
import java.util.Base64

/** 서버 개인키로 지문 문자열(UTF-8 바이트)에 서명한다. PrivateKey 빈은 LedgerConfig가 만든다. */
@Component
class Ed25519Signer(
    private val privateKey: PrivateKey,
) : RecordSigner {

    override fun sign(hashHex: String): String {
        val signature = Signature.getInstance("Ed25519")
        signature.initSign(privateKey)
        signature.update(hashHex.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(signature.sign())
    }
}
