package com.ap.sts.certificates.domain;

/**
 * SL-002 numbering mode per share class (contract NumberingConfig.mode).
 * <ul>
 *   <li>{@code Automatic}: numbers are taken from the gapless counter with no confirmation.</li>
 *   <li>{@code Manual} (pre-printed stock, e.g. VECO; S3-Q2): every print needs a Team Leader to enter
 *       and approve the number on the first pre-printed form, which must still be the next number.</li>
 * </ul>
 * In either mode a manual number needs Team Leader approval (TR-018) and must be the next number (S3-Q8).
 */
public enum NumberingMode {
    Automatic,
    Manual
}
