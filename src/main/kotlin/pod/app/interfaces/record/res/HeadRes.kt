package pod.app.interfaces.record.res

import pod.app.application.dto.HeadDto

data class HeadRes(
    val seq: Long,
    val hash: String,
) {
    companion object {
        fun from(dto: HeadDto): HeadRes {
            return HeadRes(seq = dto.seq, hash = dto.hash)
        }
    }
}
