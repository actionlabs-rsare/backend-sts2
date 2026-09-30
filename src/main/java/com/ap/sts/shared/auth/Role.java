package com.ap.sts.shared.auth;

/** System roles (BRD SL-001), matching common.openapi.yaml #/schemas/Role. */
public enum Role {
    Admin,
    User,
    FirstLevelApprover,
    SecondLevelApprover,
    TeamLeader
}
