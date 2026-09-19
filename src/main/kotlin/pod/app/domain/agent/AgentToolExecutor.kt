package pod.app.domain.agent

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import pod.app.domain.guard.PaymentGuardService
import pod.app.domain.product.Product
import pod.app.domain.product.ProductRepository
import pod.app.domain.record.AgentInfo
import pod.app.domain.record.Attempt
import pod.app.domain.record.RecordService

/**
 * AI가 도구를 부르면 실제로 실행되는 곳.
 * pay는 결제 가드를 거치고, 판정이 무엇이든 장부에 한 줄 남긴다 — "AI가 결제를 시도했다"는 사실 자체가 증거다.
 */
@Component
class AgentToolExecutor(
    private val productRepository: ProductRepository,
    private val paymentGuardService: PaymentGuardService,
    private val recordService: RecordService,
) : AgentTools {
    private val logger = LoggerFactory.getLogger(AgentToolExecutor::class.java)

    override fun searchProduct(keyword: String): List<Product> {
        val products = productRepository.findByKeyword(keyword)
        logger.info("상품 검색 keyword={} found={}", keyword, products.size)
        return products
    }

    override fun pay(agent: AgentInfo, item: String, amount: Long, merchant: String, rawRequest: String): PayResult {
        logger.info("AI 결제 시도 item={} amount={} merchant={}", item, amount, merchant)

        val verdict = paymentGuardService.judge(amount, merchant)

        val attempt = Attempt(tool = "pay", item = item, amount = amount, currency = "KRW", merchant = merchant)
        val record = recordService.append(agent, attempt, verdict.decision, verdict.reason, rawRequest)

        return PayResult(verdict.decision, verdict.reason, record.seq)
    }
}
