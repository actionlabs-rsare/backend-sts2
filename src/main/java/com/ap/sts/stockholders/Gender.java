package com.ap.sts.stockholders;

/**
 * Gender (BRD MDM-002 required field, absent on ux-06 — conflict UX-4, resolved at Gate 2).
 * Personal data: never written to logs (see {@link Pii}).
 */
public enum Gender {
    Female, Male, PreferNotToSay
}
