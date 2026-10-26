package com.viglet.turing.spring.security;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.viglet.turing.persistence.model.auth.TurUser;

@Component
public class TurAuditorAwareImpl implements AuditorAware<String> {

    public static final String ADMIN = "admin";
    public static final String PREFERRED_USERNAME = "preferred_username";

    @Override
    public @NotNull Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.of(ADMIN);
        }
        Object principal = authentication.getPrincipal();
        if (principal == null || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of(ADMIN);
        }
        // T1126 — read the principal by what it is, not by how the deployment
        // signs in: an API-key request carries the key owner's TurUser even under
        // Keycloak, and a session carries TurCustomUserDetails, a TurUser too.
        if (principal instanceof TurUser user && StringUtils.hasText(user.getUsername())) {
            return Optional.of(user.getUsername().toLowerCase());
        }
        if (principal instanceof OAuth2User user && user.getAttribute(PREFERRED_USERNAME) instanceof String name) {
            return Optional.of(name.toLowerCase());
        }
        if (principal instanceof UserDetails details) {
            return Optional.of(details.getUsername().toLowerCase());
        }
        return Optional.ofNullable(authentication.getName()).filter(StringUtils::hasText)
                .map(String::toLowerCase).or(() -> Optional.of(ADMIN));
    }
}
