package pod.app.domain.crypto

/** 지문과 서명(base64)이 짝이 맞는지 확인한다. 깨진 입력이면 예외 대신 false. */
interface SignatureVerifier {
    fun verify(hashHex: String, signature: String): Boolean
}
