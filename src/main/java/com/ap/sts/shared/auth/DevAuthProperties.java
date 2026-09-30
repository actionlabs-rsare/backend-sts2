package com.ap.sts.shared.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** sts.dev-auth.* — dev sign-in must be OFF outside dev (SECURITY-09/12). */
@ConfigurationProperties(prefix = "sts.dev-auth")
public class DevAuthProperties {
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
