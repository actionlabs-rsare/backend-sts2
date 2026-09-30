package com.ap.sts.stockholders;

import com.ap.sts.shared.error.ApiError;
import com.ap.sts.shares.ShareClassConflictException;
import com.ap.sts.shares.ShareClassController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Error mapping for the Unit S1 controllers.
 *
 * <p>The shared {@code GlobalExceptionHandler} declares a catch-all {@code Exception} handler. Two
 * advices with no declared order are consulted in an arbitrary sequence, so without
 * {@link Order} the catch-all wins and a business-rule failure surfaces as a 500. Highest
 * precedence puts this advice first for the S1 controllers only; everything it does not handle
 * still falls through to the shared handler.
 *
 * <p>Change request **CR-9**: {@code HttpRequestMethodNotSupportedException} should map to 405 in
 * the shared handler for the whole application, not just here.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {
        StockholderController.class,
        FamilyGroupController.class,
        ShareClassController.class})
public class MasterDataExceptionHandler {

    /** Share-class rule failures are the client's input problem, not a server fault. */
    @ExceptionHandler(ShareClassConflictException.class)
    public ResponseEntity<ApiError> handleShareClassConflict(ShareClassConflictException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of("validation_error", ex.getMessage()));
    }

    /**
     * An unrouted verb — most importantly {@code DELETE}, which this unit deliberately does not
     * expose (OI-19) — answers 405, not 500. A 500 would suggest the server broke; the truth is
     * that the operation does not exist.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiError.of("method_not_allowed",
                        ex.getMethod() + " is not supported on this resource"));
    }
}
