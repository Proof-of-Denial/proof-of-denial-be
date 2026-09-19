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

    // 라우팅 관련
    INVALID_REQUEST_PARAMETER("INVALID_REQUEST_PARAMETER", "요청 파라미터가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    NOT_FOUND("NOT_FOUND", "요청한 경로가 없습니다.", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "허용되지 않은 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),

    // 서버
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // 에이전트 관련
    AGENT_NOT_CONFIGURED("AGENT_NOT_CONFIGURED", "ANTHROPIC_API_KEY가 설정되지 않았습니다.", HttpStatus.SERVICE_UNAVAILABLE),
    AGENT_FAILED("AGENT_FAILED", "AI 에이전트 호출에 실패했습니다.", HttpStatus.BAD_GATEWAY),

    // 앵커 관련
    ANCHOR_NOT_FOUND("ANCHOR_NOT_FOUND", "아직 찍힌 도장이 없습니다.", HttpStatus.NOT_FOUND),
    ANCHOR_FAILED("ANCHOR_FAILED", "블록체인 기록에 실패했습니다.", HttpStatus.BAD_GATEWAY),
}
