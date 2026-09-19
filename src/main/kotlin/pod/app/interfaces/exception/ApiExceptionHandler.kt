package pod.app.interfaces.exception

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import pod.app.interfaces.common.CommonRes

@RestControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(ApiException::class)
    fun handleApiException(e: ApiException): ResponseEntity<CommonRes<Any?>> {
        return ResponseEntity
            .status(e.exceptionCode.httpStatus)
            .body(CommonRes.error(e.exceptionCode, e.detail))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<CommonRes<Any?>> {
        val firstError = e.bindingResult.fieldErrors.firstOrNull()
        val detail = if (firstError == null) null else "${firstError.field}: ${firstError.defaultMessage}"
        return ResponseEntity
            .status(ExceptionCode.VALIDATION_ERROR.httpStatus)
            .body(CommonRes.error(ExceptionCode.VALIDATION_ERROR, detail))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(e: HttpMessageNotReadableException): ResponseEntity<CommonRes<Any?>> {
        logger.warn("요청 본문을 읽을 수 없음: ${e.message}")
        return ResponseEntity
            .status(ExceptionCode.INVALID_REQUEST_BODY.httpStatus)
            .body(CommonRes.error(ExceptionCode.INVALID_REQUEST_BODY, null))
    }

    // 여기부터는 컨트롤러에 닿기 전에 스프링 자체가 던지는 예외다. 이 핸들러가 없으면 아래 Exception 캐치올이
    // 전부 삼켜서 500으로 나간다 — 클라이언트 실수(잘못된 경로 파라미터, 없는 경로, 허용 안 된 메서드)인데
    // 서버 오류로 보이면 안 되므로 각각 맞는 상태 코드로 매핑한다.
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<CommonRes<Any?>> {
        return ResponseEntity
            .status(ExceptionCode.INVALID_REQUEST_PARAMETER.httpStatus)
            .body(CommonRes.error(ExceptionCode.INVALID_REQUEST_PARAMETER, null))
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(e: NoResourceFoundException): ResponseEntity<CommonRes<Any?>> {
        return ResponseEntity
            .status(ExceptionCode.NOT_FOUND.httpStatus)
            .body(CommonRes.error(ExceptionCode.NOT_FOUND, null))
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(e: HttpRequestMethodNotSupportedException): ResponseEntity<CommonRes<Any?>> {
        return ResponseEntity
            .status(ExceptionCode.METHOD_NOT_ALLOWED.httpStatus)
            .body(CommonRes.error(ExceptionCode.METHOD_NOT_ALLOWED, null))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ResponseEntity<CommonRes<Any?>> {
        logger.error("처리되지 않은 예외", e)
        return ResponseEntity
            .status(ExceptionCode.INTERNAL_SERVER_ERROR.httpStatus)
            .body(CommonRes.error(ExceptionCode.INTERNAL_SERVER_ERROR, null))
    }
}
