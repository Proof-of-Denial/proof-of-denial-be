package pod.app.domain.guard

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pod.app.domain.record.Decision

/**
 * AI의 결제 시도를 허용할지 판정한다. 규칙은 application.yml의 guard.* 값.
 * 순서가 중요하다: 권한 → 한도 → 가게. 첫 번째로 걸리는 규칙이 사유가 된다.
 */
@Service
class PaymentGuardService(
    @Value("\${guard.payment-allowed}") private val paymentAllowed: Boolean,
    @Value("\${guard.max-amount}") private val maxAmount: Long,
    @Value("\${guard.allowed-merchants}") private val allowedMerchants: List<String>,
) {
    private val logger = LoggerFactory.getLogger(PaymentGuardService::class.java)

    fun judge(amount: Long, merchant: String): GuardVerdict {
        if (!paymentAllowed) {
            logger.info("결제 차단: 권한 없음 amount={} merchant={}", amount, merchant)
            return GuardVerdict(Decision.BLOCKED, "NO_PAYMENT_PERMISSION")
        }

        val isOverLimit = amount > maxAmount
        if (isOverLimit) {
            logger.info("결제 차단: 한도 초과 amount={} max={}", amount, maxAmount)
            return GuardVerdict(Decision.BLOCKED, "AMOUNT_OVER_LIMIT")
        }

        val isKnownMerchant = allowedMerchants.contains(merchant)
        if (!isKnownMerchant) {
            logger.info("결제 차단: 미등록 가게 merchant={}", merchant)
            return GuardVerdict(Decision.BLOCKED, "MERCHANT_NOT_ALLOWED")
        }

        logger.info("결제 허용 amount={} merchant={}", amount, merchant)
        return GuardVerdict(Decision.ALLOWED, "WITHIN_POLICY")
    }
}
