package pod.app.domain.anchor

/** 체인에 쓴 결과. */
data class ChainWriteResult(
    val txHash: String,
    val address: String,
)
