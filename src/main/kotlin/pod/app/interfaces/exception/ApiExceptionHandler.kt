package pod.app.interfaces.exception

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
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

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(e: Exception): ResponseEntity<CommonRes<Any?>> {
        logger.error("처리되지 않은 예외", e)
        return ResponseEntity
            .status(ExceptionCode.INTERNAL_SERVER_ERROR.httpStatus)
            .body(CommonRes.error(ExceptionCode.INTERNAL_SERVER_ERROR, null))
    }
}
