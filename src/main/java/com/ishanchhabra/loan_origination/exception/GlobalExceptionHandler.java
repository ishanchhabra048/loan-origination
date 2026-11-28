
package com.ishanchhabra.loan_origination.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Handle duplicate mobile number
    @ExceptionHandler(DuplicateCustomerException.class)
    public ResponseEntity<?> handleDuplicateCustomer(
            DuplicateCustomerException ex) {

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "message", ex.getMessage()
                ));
    }

    // 2. Handle invalid customer input
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleInvalidInput(
            IllegalArgumentException ex) {

        return ResponseEntity.badRequest()
                .body(Map.of(
                        "message", ex.getMessage()
                ));
    }

    // 3. Handle database errors
    @ExceptionHandler(SQLException.class)
    public ResponseEntity<?> handleDatabaseError(
            SQLException ex) {

        // MySQL duplicate-key error
        if (ex.getErrorCode() == 1062) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message", "Duplicate record already exists"
                    ));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "message", "Database operation failed"
                ));
    }
}
