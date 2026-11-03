package com.example.floodmonitor.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> api(ApiException e, HttpServletRequest request) {
        return error(e.getStatus(), e.getMessage(), request);
    }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> invalid(Exception e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "Invalid parameter: check enums, boolean, ISO-8601 dates, range and limit (1..1000)", request);
    }
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiError> format(Exception e, HttpServletRequest request) {
        return error(HttpStatus.NOT_ACCEPTABLE, "Supported formats: application/json and application/xml", request);
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> method(Exception e, HttpServletRequest request) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Public read-only API: use GET", request);
    }
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> missing(Exception e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Resource not found", request);
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request);
    }
    private ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        MediaType type = MediaType.APPLICATION_JSON;
        if (accept != null) {
            try {
                var types = MediaType.parseMediaTypes(accept);
                MediaType.sortBySpecificityAndQuality(types);
                for (var candidate : types) {
                    if (candidate.getQualityValue() <= 0) continue;
                    if (candidate.isCompatibleWith(MediaType.APPLICATION_JSON)) break;
                    if (candidate.isCompatibleWith(MediaType.APPLICATION_XML)) { type = MediaType.APPLICATION_XML; break; }
                }
            } catch (IllegalArgumentException ignored) { /* JSON fallback for invalid Accept. */ }
        }
        return ResponseEntity.status(status).contentType(type).body(
            new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
    }
}
