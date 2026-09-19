package pod.app.domain.verify

data class VerifyResult(
    val ok: Boolean,
    val count: Int,
    val head: String?,
    val problems: List<Problem>,
)
