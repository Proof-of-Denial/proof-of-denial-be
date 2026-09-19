package pod.app.interfaces.cli

import org.slf4j.LoggerFactory
import pod.app.infrastructure.crypto.Ed25519Keys
import java.nio.file.Files
import java.nio.file.Path

private val logger = LoggerFactory.getLogger("pod.app.interfaces.cli.KeygenCli")

/** 사용: ./gradlew keygen  → keys/ed25519.private, keys/ed25519.public */
fun main(args: Array<String>) {
    val dir = Path.of(args.getOrElse(0) { "keys" })
    Files.createDirectories(dir)
    val privateFile = dir.resolve("ed25519.private")
    val publicFile = dir.resolve("ed25519.public")
    if (Files.exists(privateFile)) {
        logger.info("이미 있음: {} (덮어쓰지 않음)", privateFile)
        return
    }
    val keyPair = Ed25519Keys.generate()
    Files.writeString(privateFile, Ed25519Keys.encodePrivate(keyPair.private))
    Files.writeString(publicFile, Ed25519Keys.encodePublic(keyPair.public))
    logger.info("생성: {}", privateFile)
    logger.info("생성: {}", publicFile)
}
