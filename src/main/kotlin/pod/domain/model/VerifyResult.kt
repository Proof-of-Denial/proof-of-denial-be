package pod.domain.model

/**
 * 검증에서 잡히는 문제 종류.
 *  - SEQ_GAP        순번이 건너뜀 (줄 삭제)
 *  - HASH_MISMATCH  내용이 지문과 안 맞음 (내용 수정)
 *  - PREV_MISMATCH  앞 지문이 실제 앞 기록의 지문과 안 맞음 (수정 후 재해시, 또는 삭제)
 *  - BAD_SIGNATURE  서명 불일치 (다른 키, 또는 hash 필드 수정)
 *  - HEAD_MISMATCH  마지막 지문이 외부(블록체인)에 기록된 값과 다름
 */
enum class ProblemKind { SEQ_GAP, HASH_MISMATCH, PREV_MISMATCH, BAD_SIGNATURE, HEAD_MISMATCH }

/** seq가 null이면 장부 전체에 대한 문제(HEAD_MISMATCH). */
data class Problem(val seq: Long?, val kind: ProblemKind, val detail: String)

data class VerifyResult(val ok: Boolean, val count: Int, val head: String?, val problems: List<Problem>)
