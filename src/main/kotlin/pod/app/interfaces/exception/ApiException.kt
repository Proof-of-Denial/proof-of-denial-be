package pod.app.interfaces.exception

/** 컨트롤러·파사드에서 던지는 예외. ApiExceptionHandler가 CommonRes로 바꾼다. */
class ApiException(
    val exceptionCode: ExceptionCode,
    val detail: String? = null,
) : RuntimeException(exceptionCode.message)
