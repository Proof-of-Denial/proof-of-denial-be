package pod.app.interfaces.cli

import pod.app.infrastructure.crypto.Ed25519Keys
import java.nio.file.Files
import java.nio.file.Path

/** 사용: ./gradlew keygen  → keys/ed25519.private, keys/ed25519.public */
fun main(args: Array<String>) {
    val dir = Path.of(args.getOrElse(0) { "keys" })
    Files.createDirectories(dir)
    val privateFile = dir.resolve("ed25519.private")
    val publicFile = dir.resolve("ed25519.public")
    if (Files.exists(privateFile)) {
        println("이미 있음: $privateFile (덮어쓰지 않음)")
        return
    }
    val keyPair = Ed25519Keys.generate()
    Files.writeString(privateFile, Ed25519Keys.encodePrivate(keyPair.private))
    Files.writeString(publicFile, Ed25519Keys.encodePublic(keyPair.public))
    println("생성: $privateFile")
    println("생성: $publicFile")
}
