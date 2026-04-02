package dev.gavin.runqueue.common.api

import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    companion object {
        const val BAD_REQUEST = "Bad Request"
    }

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFoundException(ex: Exception) =
        ErrorResponse(
            timestamp = Instant.now(),
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "The requested resource was not found",
        )

    @ExceptionHandler(ValidationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleValidationException(ex: Exception) = ErrorResponse(
        timestamp = Instant.now(),
        status = HttpStatus.BAD_REQUEST.value(),
        error = BAD_REQUEST,
        message = ex.message ?: "Validation error",
    )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException) = ErrorResponse(
        timestamp = Instant.now(),
        status = HttpStatus.BAD_REQUEST.value(),
        error = BAD_REQUEST,
        message = ex.bindingResult.allErrors
            .mapNotNull {
                when (it) {
                    is FieldError -> "${it.field}: ${it.defaultMessage}"
                    else -> it.defaultMessage
                }
            }
            .joinToString(", ")
            .ifBlank { "Validation error" },
    )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException) = ErrorResponse(
        timestamp = Instant.now(),
        status = HttpStatus.BAD_REQUEST.value(),
        error = BAD_REQUEST,
        message = ex.mostSpecificCause.message ?: "Malformed request body",
    )

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleException(ex: Exception) = ErrorResponse(
        timestamp = Instant.now(),
        status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
        error = "Internal Server Error",
        message = ex.message ?: "Internal Server Error",
    )
}

data class ErrorResponse(
    val timestamp: Instant,
    val status: Int,
    val error: String,
    val message: String,
)
