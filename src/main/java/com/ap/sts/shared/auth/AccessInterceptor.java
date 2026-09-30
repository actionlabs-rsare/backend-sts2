package com.ap.sts.shared.auth;

import com.ap.sts.shared.error.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Server-side enforcement of @RequiresPermission (SECURITY-08). Deny by default:
 * a handler annotated with a required permission is only allowed when the current
 * session's role is granted that permission in the AccessMatrix.
 */
@Component
public class AccessInterceptor implements HandlerInterceptor {

    private final AccessMatrix accessMatrix;

    public AccessInterceptor(AccessMatrix accessMatrix) {
        this.accessMatrix = accessMatrix;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequiresPermission required = handlerMethod.getMethodAnnotation(RequiresPermission.class);
        if (required == null) {
            return true; // no permission declared -> not access-controlled (e.g. auth, health)
        }
        Session session = CurrentUser.get();
        if (session == null) {
            throw new ForbiddenException("Authentication required");
        }
        if (!accessMatrix.allows(session.role(), required.module(), required.action())) {
            throw new ForbiddenException("Access denied for " + required.action() + " on " + required.module());
        }
        return true;
    }
}
