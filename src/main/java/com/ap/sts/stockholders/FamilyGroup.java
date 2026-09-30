package com.ap.sts.stockholders;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Family group (MDM-002-FG, OI-15). A stockholder belongs to zero or one group.
 * Grouping is an inquiry aid only — it never affects certificate or holding invariants.
 * Grouping rules still need AP Corsec sign-off (OI-15 follow-up).
 */
@Entity
@Table(name = "family_group")
public class FamilyGroup {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    protected FamilyGroup() {
        // JPA
    }

    public FamilyGroup(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
