package com.ap.sts.shared.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Resolves the dev session from the Authorization: Bearer &lt;token&gt; header and
 * exposes it via CurrentUser for the duration of the request. No token = anonymous
 * (the access interceptor then denies protected endpoints — deny by default).
 */
@Component
public class AuthFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";
    private final SessionStore sessions;

    public AuthFilter(SessionStore sessions) {
        this.sessions = sessions;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith(BEARER)) {
                String token = header.substring(BEARER.length()).trim();
                sessions.resolve(token).ifPresent(CurrentUser::set);
            }
            chain.doFilter(request, response);
        } finally {
            CurrentUser.clear();
        }
    }
}
