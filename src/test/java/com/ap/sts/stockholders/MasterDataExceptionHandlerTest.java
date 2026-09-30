package com.ap.sts.stockholders;

import com.ap.sts.shared.error.ApiError;
import com.ap.sts.shared.error.GlobalExceptionHandler;
import com.ap.sts.shares.ShareClassConflictException;
import com.ap.sts.shares.ShareClassController;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Error mapping for S1. The ordering assertion matters: without it, the shared catch-all
 * {@code Exception} handler can win and a rule failure reaches the client as a 500.
 */
class MasterDataExceptionHandlerTest {

    private final MasterDataExceptionHandler handler = new MasterDataExceptionHandler();

    @Test
    void shareClassRuleFailureIsAValidationError() {
        ResponseEntity<ApiError> response = handler.handleShareClassConflict(
                new ShareClassConflictException("Issued shares cannot exceed subscribed shares"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("validation_error", response.getBody().code());
        assertEquals("Issued shares cannot exceed subscribed shares", response.getBody().message());
    }

    @Test
    void anUnroutedVerbIsMethodNotAllowedRatherThanAServerError() {
        ResponseEntity<ApiError> response = handler.handleMethodNotAllowed(
                new HttpRequestMethodNotSupportedException("DELETE", Set.of("GET", "PUT")));

        // OI-19: delete is absent by design, so the honest answer is 405, not 500.
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("method_not_allowed", response.getBody().code());
        assertTrue(response.getBody().message().startsWith("DELETE"));
    }

    @Test
    void thisAdviceOutranksTheSharedCatchAll() {
        Order order = MasterDataExceptionHandler.class.getAnnotation(Order.class);
        assertNotNull(order, "without an explicit order the shared Exception handler can win");
        assertEquals(Ordered.HIGHEST_PRECEDENCE, order.value());
        // The shared handler deliberately has no order, which is why this one needs the highest.
        assertEquals(null, GlobalExceptionHandler.class.getAnnotation(Order.class));
    }

    @Test
    void itCoversEveryS1Controller() {
        RestControllerAdvice advice =
                MasterDataExceptionHandler.class.getAnnotation(RestControllerAdvice.class);
        assertNotNull(advice);
        assertTrue(List.of(advice.assignableTypes()).containsAll(List.of(
                StockholderController.class, FamilyGroupController.class, ShareClassController.class)));
    }
}
