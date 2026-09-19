package pod.app.domain.anchor

/** 영수증 저장소. 장부처럼 붙이기만 한다. */
interface AnchorReceiptRepository {
    fun findAll(): List<AnchorReceipt>
    fun save(receipt: AnchorReceipt)
}
