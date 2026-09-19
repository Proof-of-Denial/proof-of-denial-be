package pod.app.domain.anchor

/**
 * 블록체인에 지문을 쓰고 읽는 포트. domain은 이 인터페이스만 알고, 실제 체인 구현(xrpl4j)은
 * infrastructure/chain에 있다 — domain이 특정 체인 SDK를 import하지 않게 하기 위해서다.
 */
interface ChainAnchor {
    fun writeHead(seq: Long, hash: String): ChainWriteResult
    fun readHead(txHash: String): String?
}
