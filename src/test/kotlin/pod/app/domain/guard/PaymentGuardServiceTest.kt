package pod.app.domain.guard

import org.junit.jupiter.api.Test
import pod.app.domain.record.Decision
import kotlin.test.assertEquals

class PaymentGuardServiceTest {

    @Test
    fun `결제 권한이 없으면 금액·가게와 무관하게 NO_PAYMENT_PERMISSION`() {
        val guard = PaymentGuardService(paymentAllowed = false, maxAmount = 10000, allowedMerchants = listOf("마트A"))
        assertEquals(GuardVerdict(Decision.BLOCKED, "NO_PAYMENT_PERMISSION"), guard.judge(amount = 100, merchant = "마트A"))
    }

    @Test
    fun `권한은 있지만 한도를 넘으면 AMOUNT_OVER_LIMIT`() {
        val guard = PaymentGuardService(paymentAllowed = true, maxAmount = 10000, allowedMerchants = listOf("마트A"))
        assertEquals(GuardVerdict(Decision.BLOCKED, "AMOUNT_OVER_LIMIT"), guard.judge(amount = 10001, merchant = "마트A"))
        assertEquals(GuardVerdict(Decision.ALLOWED, "WITHIN_POLICY"), guard.judge(amount = 10000, merchant = "마트A"))
    }

    @Test
    fun `권한 있고 한도 안이라도 미등록 가게면 MERCHANT_NOT_ALLOWED`() {
        val guard = PaymentGuardService(paymentAllowed = true, maxAmount = 10000, allowedMerchants = listOf("마트A"))
        assertEquals(GuardVerdict(Decision.BLOCKED, "MERCHANT_NOT_ALLOWED"), guard.judge(amount = 5980, merchant = "마트B"))
    }

    @Test
    fun `전부 통과하면 ALLOWED`() {
        val guard = PaymentGuardService(paymentAllowed = true, maxAmount = 10000, allowedMerchants = listOf("마트A", "마트B"))
        assertEquals(GuardVerdict(Decision.ALLOWED, "WITHIN_POLICY"), guard.judge(amount = 5980, merchant = "마트B"))
    }
}
