package org.example.trafficsim.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Maps exceptions thrown during request handling to appropriate HTTP responses.
 *
 * 400 Bad Request is returned for:
 *   - malformed / unparseable JSON (HttpMessageNotReadableException — handled by Spring by default,
 *     but listed here for clarity and custom message format)
 *   - Bean Validation failures (@Valid on @RequestBody)
 *   - Unknown command type or invalid road / lane values (IllegalArgumentException)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Unknown command type or invalid road name thrown by the simulation engine. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(ex.getMessage()));
    }

    /** Bean Validation failures — missing / blank required fields. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(message));
    }

    /** Malformed JSON body (Spring normally returns 400 for this, but we unify the response shape). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Malformed JSON request: " + ex.getMostSpecificCause().getMessage()));
    }

    public record ErrorResponse(String error) {}
}
