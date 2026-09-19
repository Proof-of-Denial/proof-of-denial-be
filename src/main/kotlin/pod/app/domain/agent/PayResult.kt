package pod.app.domain.agent

import pod.app.domain.record.Decision

/** pay 도구의 실행 결과. 장부에 적힌 순번을 같이 돌려줘서 AI가 사용자에게 알릴 수 있게 한다. */
data class PayResult(
    val decision: Decision,
    val reason: String,
    val recordSeq: Long,
)
