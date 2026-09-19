package pod.app.domain.product

interface ProductRepository {
    fun findByKeyword(keyword: String): List<Product>
}
