package pod.app.domain.product

/** 데모용 상품. 가격은 원 단위. */
data class Product(
    val name: String,
    val price: Long,
    val merchant: String,
)
