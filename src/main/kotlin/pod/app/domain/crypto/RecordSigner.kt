package pod.app.domain.crypto

/** 지문(hash hex)에 서버 서명을 붙인다. 결과는 base64. */
interface RecordSigner {
    fun sign(hashHex: String): String
}
