package pod.app.infrastructure.crypto

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Ed25519SignerTest {
    private val hash = "e7b2" + "0".repeat(60)

    @Test
    fun `서명하고 검증하면 true`() {
        val keyPair = Ed25519Keys.generate()
        val signature = Ed25519Signer(keyPair.private).sign(hash)
        assertTrue(Ed25519SignatureVerifier(keyPair.public).verify(hash, signature))
    }

    @Test
    fun `해시가 한 글자라도 다르면 false`() {
        val keyPair = Ed25519Keys.generate()
        val signature = Ed25519Signer(keyPair.private).sign(hash)
        assertFalse(Ed25519SignatureVerifier(keyPair.public).verify("f" + hash.drop(1), signature))
    }

    @Test
    fun `다른 키의 공개키로는 false`() {
        val a = Ed25519Keys.generate()
        val b = Ed25519Keys.generate()
        val signature = Ed25519Signer(a.private).sign(hash)
        assertFalse(Ed25519SignatureVerifier(b.public).verify(hash, signature))
    }

    @Test
    fun `base64로 내보냈다 다시 읽어도 동작`() {
        val keyPair = Ed25519Keys.generate()
        val privateKey = Ed25519Keys.decodePrivate(Ed25519Keys.encodePrivate(keyPair.private))
        val publicKey = Ed25519Keys.decodePublic(Ed25519Keys.encodePublic(keyPair.public))
        val signature = Ed25519Signer(privateKey).sign(hash)
        assertTrue(Ed25519SignatureVerifier(publicKey).verify(hash, signature))
    }

    @Test
    fun `깨진 서명 문자열이면 예외 대신 false`() {
        val keyPair = Ed25519Keys.generate()
        assertFalse(Ed25519SignatureVerifier(keyPair.public).verify(hash, "not-base64!!"))
    }
}
