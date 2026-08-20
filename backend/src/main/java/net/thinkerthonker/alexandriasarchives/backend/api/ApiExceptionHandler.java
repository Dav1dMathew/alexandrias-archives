package net.thinkerthonker.alexandriasarchives.backend.api;

import net.thinkerthonker.alexandriasarchives.backend.service.AiServiceUnavailableException;
import net.thinkerthonker.alexandriasarchives.backend.service.InvalidAiResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidRequest(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream().findFirst()
                .map(error -> error.getDefaultMessage()).orElse("invalid request");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({AiServiceUnavailableException.class, InvalidAiResponseException.class})
    ResponseEntity<ApiError> badGateway(RuntimeException exception) {
        return error(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message));
    }

    record ApiError(Instant timestamp, int status, String error, String message) {}
}
