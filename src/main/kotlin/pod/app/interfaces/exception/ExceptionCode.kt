package pod.app.interfaces.exception

import org.springframework.http.HttpStatus

enum class ExceptionCode(
    val code: String,
    val message: String,
    val httpStatus: HttpStatus,
) {
    // 요청 관련
    INVALID_REQUEST_BODY("INVALID_REQUEST_BODY", "요청 본문이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR("VALIDATION_ERROR", "입력값 검증에 실패했습니다.", HttpStatus.BAD_REQUEST),

    // 장부 관련
    RECORD_NOT_FOUND("RECORD_NOT_FOUND", "해당 순번의 기록이 없습니다.", HttpStatus.NOT_FOUND),
    LEDGER_EMPTY("LEDGER_EMPTY", "장부가 비어 있습니다.", HttpStatus.NOT_FOUND),

    // 서버
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
}
