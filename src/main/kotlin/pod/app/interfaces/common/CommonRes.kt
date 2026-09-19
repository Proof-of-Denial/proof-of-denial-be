package pod.app.interfaces.common

import com.fasterxml.jackson.annotation.JsonInclude
import pod.app.interfaces.exception.ExceptionCode
import pod.app.interfaces.exception.ExceptionMessage

/** 공통 API 응답 봉투. 실패 시 data는 null이고 exception에 코드·메시지가 들어간다. */
data class CommonRes<T>(
    val resultType: ResultType,
    val data: T?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val exception: ExceptionMessage? = null,
) {
    companion object {
        fun <T> success(data: T): CommonRes<T> {
            return CommonRes(ResultType.SUCCESS, data, null)
        }

        fun error(code: ExceptionCode, detail: String?): CommonRes<Any?> {
            val message = if (detail == null) code.message else detail
            return CommonRes(ResultType.FAIL, null, ExceptionMessage(code.code, message))
        }
    }
}
