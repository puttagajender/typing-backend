package com.brothers.typing.controller;

import com.brothers.typing.learning.service.ExerciseGenerationException;
import com.brothers.typing.learning.recovery.service.WeakKeyRecoveryException;
import com.brothers.typing.learning.coach.service.CoachingRequestException;
import com.brothers.typing.learning.coach.service.CoachingUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        log.info("Exception occurred: type=validation, errors={}",
                exception.getBindingResult().getErrorCount());
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return errorResponse(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException exception) {
        log.info("Exception occurred: type=illegal-argument");
        return errorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(ExerciseGenerationException.class)
    public ResponseEntity<Map<String, Object>> handleExerciseGeneration(
            ExerciseGenerationException exception) {
        log.info("Exception occurred: type=exercise-generation");
        return errorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(WeakKeyRecoveryException.class)
    public ResponseEntity<Map<String, Object>> handleWeakKeyRecovery(
            WeakKeyRecoveryException exception) {
        log.info("Exception occurred: type=weak-key-recovery");
        return errorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CoachingRequestException.class)
    public ResponseEntity<Map<String, Object>> handleCoachingRequest(
            CoachingRequestException exception) {
        log.info("Exception occurred: type=coaching-request");
        return errorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CoachingUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleCoachingUnavailable(
            CoachingUnavailableException exception) {
        log.error("Coaching unavailable after AI and fallback processing");
        return errorResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "Coaching is temporarily unavailable", Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableMessage(
            HttpMessageNotReadableException exception) {
        log.info("Exception occurred: type=unreadable-request");
        return errorResponse(HttpStatus.BAD_REQUEST, "Request body is missing or malformed", Map.of());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception) {
        log.info("Exception occurred: type=unsupported-media-type");
        return errorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type is not supported", Map.of());
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNotFound(Exception exception) {
        log.info("Exception occurred: type=not-found");
        return errorResponse(HttpStatus.NOT_FOUND, "Resource not found", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception exception) {
        log.error("Unexpected exception: type={}", exception.getClass().getName(), exception);
        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                Map.of());
    }

    private ResponseEntity<Map<String, Object>> errorResponse(
            HttpStatus status, String message, Map<String, String> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        if (!errors.isEmpty()) {
            body.put("validationErrors", errors);
        }
        return ResponseEntity.status(status).body(body);
    }
}
