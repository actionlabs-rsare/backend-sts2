package com.ap.sts.certificates.web;

import com.ap.sts.certificates.domain.CertificateRuleException;
import com.ap.sts.shared.error.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Error mapping for Unit S3 controllers only (scoped by package, runs before the shared handler).
 * Uses the common Error schema. Forbidden/NotFound/field validation still go to the shared handler.
 * Without this, malformed JSON or an unknown enum value would fall into the shared catch-all (500).
 */
@RestControllerAdvice(basePackages = "com.ap.sts.certificates")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CertificatesExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CertificatesExceptionHandler.class);

    @ExceptionHandler(CertificateRuleException.class)
    public ResponseEntity<ApiError> handleRule(CertificateRuleException ex) {
        List<ApiError.FieldError> fields = ex.field() == null ? List.of()
                : List.of(new ApiError.FieldError(ex.field(), ex.getMessage()));
        return ResponseEntity.status(ex.status()).body(new ApiError(ex.code(), ex.getMessage(), fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(ApiError.of("validation_error",
                "The request body could not be read. Send JSON; mode must be PrePrintedForm or SystemTemplate, "
                        + "and numbering mode Automatic or Manual."));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ApiError> handleBadParameter(Exception ex) {
        String name = ex instanceof MethodArgumentTypeMismatchException m ? m.getName()
                : ((MissingServletRequestParameterException) ex).getParameterName();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ApiError("validation_error",
                "Parameter '" + name + "' is missing or not valid.",
                List.of(new ApiError.FieldError(name, "missing or not valid"))));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException ex) {
        // Unique/number/sequence guard fired (e.g. two prints raced). Details stay in the log only.
        log.warn("certificate integrity conflict: {}", ex.getMostSpecificCause().getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of("conflict",
                "The certificate numbers changed while this request ran (another print may have finished first). "
                        + "Reload and try again; a reprint keeps the numbers already assigned."));
    }
}
