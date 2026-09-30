package com.ap.sts.shared.auth;

import java.util.Set;

/** Current session + effective permissions (common.openapi.yaml #/schemas/Session). */
public record Session(String userId, String name, Role role, Set<Permission> permissions) {
}
