package com.ap.sts.shared.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessMatrixTest {

    private final AccessMatrix matrix = new AccessMatrix();

    @Test
    void adminHasFullCompanyRights() {
        assertTrue(matrix.allows(Role.Admin, "company", Permission.add));
        assertTrue(matrix.allows(Role.Admin, "company", Permission.change));
        assertTrue(matrix.allows(Role.Admin, "company", Permission.view));
        assertTrue(matrix.allows(Role.Admin, "company", Permission.list));
    }

    @Test
    void noDeletePermissionExists_noRecordDeletion() {
        // OI-19 / Gate 3 Q4: business records are never hard-deleted. The 'delete' right must not
        // exist in the vocabulary — deactivation/void/cancel is a 'change'. Enforced structurally.
        for (Permission p : Permission.values()) {
            assertFalse("delete".equals(p.name()),
                    "Permission enum must not contain 'delete' (no-record-deletion, OI-19)");
        }
    }

    @Test
    void approverCanApproveButNotAddTransaction() {
        assertTrue(matrix.allows(Role.FirstLevelApprover, "transaction", Permission.approve));
        assertFalse(matrix.allows(Role.FirstLevelApprover, "transaction", Permission.add));
    }

    @Test
    void unknownModuleIsDeniedByDefault() {
        assertFalse(matrix.allows(Role.Admin, "nonexistent", Permission.view));
    }

    @Test
    void nullsAreDenied() {
        assertFalse(matrix.allows(null, "company", Permission.view));
        assertFalse(matrix.allows(Role.Admin, null, Permission.view));
        assertFalse(matrix.allows(Role.Admin, "company", null));
    }
}
