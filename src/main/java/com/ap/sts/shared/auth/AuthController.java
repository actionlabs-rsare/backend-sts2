package com.ap.sts.shared.auth;

import com.ap.sts.shared.error.ForbiddenException;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dev sign-in (DEV-AUTH) + current session. Disabled outside dev by config. */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AccessMatrix accessMatrix;
    private final SessionStore sessions;
    private final DevAuthProperties devAuth;

    public AuthController(AccessMatrix accessMatrix, SessionStore sessions, DevAuthProperties devAuth) {
        this.accessMatrix = accessMatrix;
        this.sessions = sessions;
        this.devAuth = devAuth;
    }

    public record DevLoginRequest(@NotNull Role role) {
    }

    /** Session view plus the bearer token to use on subsequent calls (dev only). */
    public record SessionResponse(String userId, String name, Role role,
                                  java.util.Set<Permission> permissions, String token) {
    }

    @PostMapping("/auth/dev-login")
    public ResponseEntity<SessionResponse> devLogin(@RequestBody DevLoginRequest request) {
        if (!devAuth.isEnabled()) {
            throw new ForbiddenException("Dev sign-in is disabled in this environment");
        }
        Role role = request.role();
        Session session = new Session(
                "dev-" + role.name().toLowerCase(),
                role.name() + " (dev)",
                role,
                accessMatrix.permissionsFor(role));
        String token = sessions.issue(session);
        return ResponseEntity.ok(new SessionResponse(
                session.userId(), session.name(), session.role(), session.permissions(), token));
    }

    @GetMapping("/me")
    public Session me() {
        Session session = CurrentUser.get();
        if (session == null) {
            throw new ForbiddenException("No active session");
        }
        return session;
    }
}
