package pod.app.domain.record

/** AI가 무엇을 하려 했나. */
data class Attempt(
    val tool: String,
    val item: String,
    val amount: Long,
    val currency: String,
    val merchant: String,
)
