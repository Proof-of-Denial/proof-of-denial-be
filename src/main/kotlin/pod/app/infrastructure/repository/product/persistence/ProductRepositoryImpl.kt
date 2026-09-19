package pod.app.infrastructure.repository.product.persistence

import org.springframework.stereotype.Repository
import pod.app.domain.product.Product
import pod.app.domain.product.ProductRepository

/** 데모용 상품 3개를 코드에 박아둔다. DB는 해커톤 범위 밖. */
@Repository
class ProductRepositoryImpl : ProductRepository {

    private val products: List<Product> = listOf(
        Product(name = "계란 30구", price = 5980, merchant = "마트A"),
        Product(name = "우유 1L", price = 3200, merchant = "마트A"),
        Product(name = "식빵", price = 2500, merchant = "마트A"),
    )

    override fun findByKeyword(keyword: String): List<Product> {
        val found = ArrayList<Product>()
        for (product in products) {
            if (product.name.contains(keyword)) {
                found.add(product)
            }
        }
        return found
    }
}
