package com.ap.sts.shared.auth;

/**
 * Access-matrix rights (BRD SL-005), matching common.openapi.yaml Session.permissions.
 * NOTE (OI-19, Gate 3 Q4 "no record deletion"): there is deliberately NO {@code delete} right.
 * Business records are never hard-deleted — lifecycle is expressed by status changes
 * (deactivate / void / cancel / reject), which use {@code change}. Do not reintroduce delete.
 */
public enum Permission {
    add, change, view, list, post, approve
}
