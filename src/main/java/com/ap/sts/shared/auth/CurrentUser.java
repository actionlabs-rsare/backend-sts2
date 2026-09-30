package com.ap.sts.shared.auth;

/** Per-request holder for the resolved session. Set by AuthFilter, cleared after the request. */
public final class CurrentUser {

    private static final ThreadLocal<Session> HOLDER = new ThreadLocal<>();

    private CurrentUser() {
    }

    public static void set(Session session) {
        HOLDER.set(session);
    }

    public static Session get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
