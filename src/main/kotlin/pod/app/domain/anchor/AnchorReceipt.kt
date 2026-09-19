package pod.app.domain.anchor

/** 체인에 도장을 찍은 뒤 남기는 영수증. */
data class AnchorReceipt(
    val seq: Long,
    val hash: String,
    val txHash: String,
    val address: String,
    val anchoredAt: String,
)
