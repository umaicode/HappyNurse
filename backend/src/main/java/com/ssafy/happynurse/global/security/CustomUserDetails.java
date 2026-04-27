package com.ssafy.happynurse.global.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Long practitionerId;
    private final String employeeNumber;
    private final String name;
    private final String role;
    private final String sessionId;
    private final Long organizationId;
    private final Long wardId;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    @Override
    public String getUsername() {
        return employeeNumber;
    }

    @Override
    public String getPassword() {
        return null;
    }
}
