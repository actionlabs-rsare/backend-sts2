package com.ap.sts.stockholders;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request/response DTOs for {@code contracts/stockholders.openapi.yaml}.
 *
 * <p>Required fields mirror the contract's {@code StockholderInput.required} list:
 * name, type, tin, nationality, gender, email (BRD MDM-002; gender + email are the UX-4 additions).
 */
public final class StockholderDtos {

    private StockholderDtos() {
    }

    /**
     * Create/update payload.
     *
     * <p>{@code active} is <b>not</b> in the frozen contract yet — it carries the
     * deactivate/reactivate lifecycle that replaces deletion (OI-19). A null value leaves the
     * current state untouched, so existing contract clients behave exactly as documented.
     * Raised as change request CR-3 in the unit audit.
     */
    public record StockholderInput(
            @NotBlank @Size(max = 200) String name,
            @NotNull StockholderType type,
            @NotBlank @Size(max = 40) String tin,
            @NotBlank @Size(max = 60) String nationality,
            @NotNull Gender gender,
            @NotBlank @Email @Size(max = 200) String email,
            @Size(max = 40) String mobile,
            @Size(max = 300) String address,
            CorporationType corporationType,
            String familyGroupId,
            Boolean active) {
    }

    /**
     * Full stockholder view. Role-restricted read (SL-005); never logged (see {@link Pii}).
     *
     * <p>{@code sharesHeld} is additive to the frozen contract (change request CR-4) and is only
     * populated by the family-group inquiry, which needs combined shareholdings per group.
     * Its source is S2's holding ledger; until Wave 2 it comes from {@link HoldingsLookup}'s
     * fixture, so a null means "not computed for this view" and never "zero shares".
     */
    public record StockholderResponse(
            String stockholderCode,
            String name,
            StockholderType type,
            String tin,
            String nationality,
            Gender gender,
            String email,
            String mobile,
            String address,
            CorporationType corporationType,
            String familyGroupId,
            boolean active,
            Long sharesHeld) {

        public static StockholderResponse from(Stockholder s) {
            return from(s, null);
        }

        public static StockholderResponse from(Stockholder s, Long sharesHeld) {
            return new StockholderResponse(
                    s.getStockholderCode(), s.getName(), s.getType(), s.getTin(), s.getNationality(),
                    s.getGender(), s.getEmail(), s.getMobile(), s.getAddress(), s.getCorporationType(),
                    s.getFamilyGroupId(), s.isActive(), sharesHeld);
        }
    }

    /** Family group (MDM-002-FG). */
    public record FamilyGroupResponse(String id, String name) {

        public static FamilyGroupResponse from(FamilyGroup g) {
            return new FamilyGroupResponse(g.getId(), g.getName());
        }
    }

    public record FamilyGroupInput(@NotBlank @Size(max = 200) String name) {
    }
}
