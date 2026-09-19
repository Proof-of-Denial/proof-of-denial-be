package pod.app.domain.guard

import pod.app.domain.record.Decision

/** 결제 가드의 판정 결과. reason은 장부에 그대로 적힌다. */
data class GuardVerdict(
    val decision: Decision,
    val reason: String,
)
