package pod.app.domain.crypto

import pod.app.domain.record.HashBody

/** 기록의 지문(SHA-256 hex 64자)을 만든다. 서버와 검증 CLI가 반드시 같은 구현을 써야 한다. */
interface RecordHasher {
    fun hashOf(body: HashBody): String
}
