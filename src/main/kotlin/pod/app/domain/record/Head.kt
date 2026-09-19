package pod.app.domain.record

/** 장부 마지막 기록의 순번과 지문. 나중에 블록체인에 올릴 값. */
data class Head(
    val seq: Long,
    val hash: String,
)
