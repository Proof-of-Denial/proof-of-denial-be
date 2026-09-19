package pod

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ProofOfDenialApplication

fun main(args: Array<String>) {
    runApplication<ProofOfDenialApplication>(*args)
}
