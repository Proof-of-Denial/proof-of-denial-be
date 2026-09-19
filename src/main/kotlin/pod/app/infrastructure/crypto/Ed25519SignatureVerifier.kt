package pod.app.infrastructure.crypto

import pod.app.domain.crypto.SignatureVerifier
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

/** 공개키만으로 서명을 확인한다. 검증 CLI가 Spring 없이 직접 만들어 쓴다. */
class Ed25519SignatureVerifier(
    private val publicKey: PublicKey,
) : SignatureVerifier {

    override fun verify(hashHex: String, signature: String): Boolean {
        return try {
            val verifier = Signature.getInstance("Ed25519")
            verifier.initVerify(publicKey)
            verifier.update(hashHex.toByteArray(Charsets.UTF_8))
            verifier.verify(Base64.getDecoder().decode(signature.trim()))
        } catch (e: Exception) {
            // base64가 깨졌거나 길이가 안 맞는 서명 — 위조로 취급
            false
        }
    }
}
