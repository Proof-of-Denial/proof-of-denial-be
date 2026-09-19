package pod.app.domain.verify

/** seq가 null이면 장부 전체에 대한 문제(HEAD_MISMATCH). */
data class Problem(
    val seq: Long?,
    val kind: ProblemKind,
    val detail: String,
)
