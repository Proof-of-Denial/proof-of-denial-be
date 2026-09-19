package pod.app.application.dto

import pod.app.domain.anchor.AnchorReceipt

data class AnchorReceiptDto(
    val seq: Long,
    val hash: String,
    val txHash: String,
    val address: String,
    val anchoredAt: String,
) {
    companion object {
        fun from(receipt: AnchorReceipt): AnchorReceiptDto {
            return AnchorReceiptDto(
                seq = receipt.seq,
                hash = receipt.hash,
                txHash = receipt.txHash,
                address = receipt.address,
                anchoredAt = receipt.anchoredAt,
            )
        }
    }
}
