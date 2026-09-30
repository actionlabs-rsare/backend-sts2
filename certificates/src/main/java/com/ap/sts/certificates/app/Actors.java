package com.ap.sts.certificates.app;

import com.ap.sts.shared.auth.CurrentUser;
import com.ap.sts.shared.auth.Session;

/** Who is acting, for audit and "printed by" fields. Resolved server-side, never from client input (T2). */
final class Actors {

    private Actors() {
    }

    static String current() {
        Session session = CurrentUser.get();
        return session == null ? "system" : session.userId();
    }
}
