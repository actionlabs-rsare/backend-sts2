package com.ap.sts.shared.auth;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session store for the dev sign-in only. Real auth (PingOne, D6)
 * replaces this behind the same boundary; nothing else depends on the storage.
 */
@Component
public class SessionStore {

    private final ConcurrentHashMap<String, Session> byToken = new ConcurrentHashMap<>();

    public String issue(Session session) {
        String token = UUID.randomUUID().toString();
        byToken.put(token, session);
        return token;
    }

    public Optional<Session> resolve(String token) {
        if (token == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byToken.get(token));
    }

    public void revoke(String token) {
        if (token != null) {
            byToken.remove(token);
        }
    }
}
