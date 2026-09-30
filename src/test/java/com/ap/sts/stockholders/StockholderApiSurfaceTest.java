package com.ap.sts.stockholders;

import com.ap.sts.shared.auth.RequiresPermission;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural guarantees for the S1 API surface. These assert the rules that are easy to break by
 * adding a handler and forgetting the annotation:
 *
 * <ul>
 *   <li><b>SL-005 / SECURITY-08:</b> every routed handler declares a permission, so the
 *       deny-by-default interceptor covers it. An unannotated handler is treated as public.</li>
 *   <li><b>OI-19 / Gate 3 Q4:</b> no delete mapping exists on any S1 controller.</li>
 * </ul>
 */
class StockholderApiSurfaceTest {

    private static final List<Class<?>> CONTROLLERS =
            List.of(StockholderController.class, FamilyGroupController.class);

    private static final List<Class<? extends Annotation>> MAPPINGS = List.of(
            RequestMapping.class, GetMapping.class, PostMapping.class,
            PutMapping.class, PatchMapping.class, DeleteMapping.class);

    private static boolean isHandler(Method method) {
        return MAPPINGS.stream().anyMatch(method::isAnnotationPresent);
    }

    @Test
    void everyHandlerDeclaresAPermission() {
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers()) || !isHandler(method)) {
                    continue;
                }
                assertTrue(method.isAnnotationPresent(RequiresPermission.class),
                        controller.getSimpleName() + "." + method.getName()
                                + " is routed but declares no permission — it would be reachable "
                                + "without an access check (SL-005)");
            }
        }
    }

    @Test
    void noDeleteMappingExists() {
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                assertFalse(method.isAnnotationPresent(DeleteMapping.class),
                        controller.getSimpleName() + "." + method.getName()
                                + " exposes a delete — business records are never hard-deleted "
                                + "(OI-19). Use a status change instead.");
            }
        }
    }

    @Test
    void atLeastOneHandlerWasActuallyInspected() {
        // Guards the two tests above against silently passing if the reflection stops matching.
        long handlers = CONTROLLERS.stream()
                .flatMap(c -> List.of(c.getDeclaredMethods()).stream())
                .filter(StockholderApiSurfaceTest::isHandler)
                .count();
        assertTrue(handlers >= 7, "expected the S1 handlers to be found, saw " + handlers);
    }
}
