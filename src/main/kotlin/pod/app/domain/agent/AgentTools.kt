package pod.app.domain.agent

import pod.app.domain.product.Product
import pod.app.domain.record.AgentInfo

/** AI가 부를 수 있는 도구 두 개. 실제 일은 도메인이 하고, LLM 쪽은 이 인터페이스만 안다. */
interface AgentTools {
    fun searchProduct(keyword: String): List<Product>
    fun pay(agent: AgentInfo, item: String, amount: Long, merchant: String, rawRequest: String): PayResult
}
