package com.ap.sts.shared.auth;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Deny-by-default access matrix (SL-005 / SECURITY-08). Maps role -> module -> permissions.
 * Provisional matrix (open item Q9) — confirm the approved matrix with AP later.
 * Modules are coarse strings ("company", "stockholder", "transaction", "certificate", "audit").
 */
@Component
public class AccessMatrix {

    private final Map<Role, Map<String, Set<Permission>>> matrix = new EnumMap<>(Role.class);

    public AccessMatrix() {
        // Admin: full rights on every in-sprint module.
        // OI-19 (Gate 3 Q4 "no record deletion"): Permission has no 'delete', so allOf() cannot grant it.
        for (String m : new String[]{"company", "stockholder", "share", "transaction", "certificate", "audit"}) {
            grant(Role.Admin, m, EnumSet.allOf(Permission.class));
        }
        // User: maintain master data + create/list transactions; read audit.
        grant(Role.User, "company", EnumSet.of(Permission.add, Permission.change, Permission.view, Permission.list));
        grant(Role.User, "stockholder", EnumSet.of(Permission.add, Permission.change, Permission.view, Permission.list));
        grant(Role.User, "share", EnumSet.of(Permission.add, Permission.change, Permission.view, Permission.list));
        grant(Role.User, "transaction", EnumSet.of(Permission.add, Permission.view, Permission.list));
        // CR-S3-03 (Gate-answered): certificate:add lets User print / request a certificate number
        // (was Admin-only). Approval of a manual number stays with approve (Admin/Team Leader).
        grant(Role.User, "certificate", EnumSet.of(Permission.add, Permission.view, Permission.list));
        grant(Role.User, "audit", EnumSet.of(Permission.view, Permission.list));
        // Approvers: view/list + approve transactions.
        grant(Role.FirstLevelApprover, "transaction", EnumSet.of(Permission.view, Permission.list, Permission.approve));
        grant(Role.SecondLevelApprover, "transaction", EnumSet.of(Permission.view, Permission.list, Permission.approve));
        // Team Leader: approve + post (overrides/voids).
        grant(Role.TeamLeader, "transaction", EnumSet.of(Permission.view, Permission.list, Permission.approve, Permission.post));
        // CR-S3-03: Team Leader can print (add) as well as approve manual-number overrides.
        grant(Role.TeamLeader, "certificate", EnumSet.of(Permission.add, Permission.view, Permission.list, Permission.approve));
    }

    private void grant(Role role, String module, Set<Permission> perms) {
        matrix.computeIfAbsent(role, r -> new HashMap<>()).put(module, perms);
    }

    /** True only if the role is explicitly granted the permission on the module (deny by default). */
    public boolean allows(Role role, String module, Permission permission) {
        if (role == null || module == null || permission == null) {
            return false;
        }
        Map<String, Set<Permission>> byModule = matrix.get(role);
        if (byModule == null) {
            return false;
        }
        return byModule.getOrDefault(module, Collections.emptySet()).contains(permission);
    }

    /** Flattened set of permissions across all modules, for the Session view. */
    public Set<Permission> permissionsFor(Role role) {
        Set<Permission> all = EnumSet.noneOf(Permission.class);
        Map<String, Set<Permission>> byModule = matrix.get(role);
        if (byModule != null) {
            byModule.values().forEach(all::addAll);
        }
        return all;
    }
}
