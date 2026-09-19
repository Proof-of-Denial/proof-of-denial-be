package pod.app.domain.anchor

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pod.app.domain.record.RecordService
import pod.app.interfaces.exception.ApiException
import pod.app.interfaces.exception.ExceptionCode
import java.time.Clock
import java.time.OffsetDateTime

/** 장부의 마지막 지문을 체인에 올리고(도장) 영수증을 남긴다. */
@Service
class AnchorService(
    private val recordService: RecordService,
    private val chainAnchor: ChainAnchor,
    private val anchorReceiptRepository: AnchorReceiptRepository,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(AnchorService::class.java)

    fun anchorNow(): AnchorReceipt {
        val head = recordService.head()
        if (head == null) {
            throw ApiException(ExceptionCode.LEDGER_EMPTY)
        }
        val written = chainAnchor.writeHead(head.seq, head.hash)
        val anchoredAt = OffsetDateTime.now(clock).toString()
        val receipt = AnchorReceipt(head.seq, head.hash, written.txHash, written.address, anchoredAt)
        anchorReceiptRepository.save(receipt)
        logger.info("도장 찍음 seq={} hash={} tx={}", head.seq, head.hash, written.txHash)
        return receipt
    }

    fun latest(): AnchorReceipt? {
        val receipts = anchorReceiptRepository.findAll()
        if (receipts.isEmpty()) {
            return null
        }
        return receipts[receipts.size - 1]
    }
}
