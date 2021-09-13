package com.julenka.api.primitiveBank.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

@RequiredArgsConstructor
public enum Roles implements GrantedAuthority {

    USER("User"), ADMIN("Admin");

    private final String role;

    @Override
    public String getAuthority() {
        return name();
    }
}
