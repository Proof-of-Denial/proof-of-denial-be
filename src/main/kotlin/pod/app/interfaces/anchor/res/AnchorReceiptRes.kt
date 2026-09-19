package pod.app.interfaces.anchor.res

import pod.app.application.dto.AnchorReceiptDto

data class AnchorReceiptRes(
    val seq: Long,
    val hash: String,
    val txHash: String,
    val address: String,
    val anchoredAt: String,
) {
    companion object {
        fun from(dto: AnchorReceiptDto): AnchorReceiptRes {
            return AnchorReceiptRes(
                seq = dto.seq,
                hash = dto.hash,
                txHash = dto.txHash,
                address = dto.address,
                anchoredAt = dto.anchoredAt,
            )
        }
    }
}
