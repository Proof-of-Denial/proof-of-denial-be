package pod.app.infrastructure.crypto

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** JDK 내장 Ed25519 키 생성과 base64 인코딩/디코딩. 개인키는 PKCS#8, 공개키는 X.509. */
object Ed25519Keys {

    fun generate(): KeyPair {
        return KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
    }

    fun encodePrivate(key: PrivateKey): String {
        return Base64.getEncoder().encodeToString(key.encoded)
    }

    fun encodePublic(key: PublicKey): String {
        return Base64.getEncoder().encodeToString(key.encoded)
    }

    fun decodePrivate(base64: String): PrivateKey {
        val bytes = Base64.getDecoder().decode(base64.trim())
        return KeyFactory.getInstance("Ed25519").generatePrivate(PKCS8EncodedKeySpec(bytes))
    }

    fun decodePublic(base64: String): PublicKey {
        val bytes = Base64.getDecoder().decode(base64.trim())
        return KeyFactory.getInstance("Ed25519").generatePublic(X509EncodedKeySpec(bytes))
    }
}
