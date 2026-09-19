package pod.app.interfaces.cli

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import pod.app.infrastructure.crypto.Ed25519Keys
import pod.app.infrastructure.crypto.Ed25519SignatureVerifier
import pod.app.infrastructure.crypto.Ed25519Signer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KeygenCliTest {
    @Test
    fun `키 두 파일을 만들고, 두 번째 실행은 덮어쓰지 않는다`(@TempDir dir: Path) {
        main(arrayOf(dir.toString()))
        val privateFile = dir.resolve("ed25519.private")
        val publicFile = dir.resolve("ed25519.public")
        assertTrue(Files.exists(privateFile))
        assertTrue(Files.exists(publicFile))

        val privateKey = Ed25519Keys.decodePrivate(Files.readString(privateFile))
        val publicKey = Ed25519Keys.decodePublic(Files.readString(publicFile))
        val hash = "ab".repeat(32)
        val signature = Ed25519Signer(privateKey).sign(hash)
        assertTrue(Ed25519SignatureVerifier(publicKey).verify(hash, signature))

        val before = Files.readString(privateFile)
        main(arrayOf(dir.toString()))
        assertEquals(before, Files.readString(privateFile))
    }
}
