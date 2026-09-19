package pod

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ProofOfDenialApplication

fun main(args: Array<String>) {
    // XRPL 테스트넷 서버들이 중간 인증서를 안 내려줘서 JVM 기본 검증이 실패한다.
    // 이 옵션은 인증서를 AIA로 보충해 체인을 완성할 뿐 검증을 끄지 않는다.
    // IDE·jar 실행에서도 동작해야 하므로 실행 옵션이 아니라 여기서 켠다.
    System.setProperty("com.sun.security.enableAIAcaIssuers", "true")

    runApplication<ProofOfDenialApplication>(*args)
}
