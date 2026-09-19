package pod.app.application.dto

import pod.app.domain.record.Head

data class HeadDto(
    val seq: Long,
    val hash: String,
) {
    companion object {
        fun from(head: Head): HeadDto {
            return HeadDto(seq = head.seq, hash = head.hash)
        }
    }
}
