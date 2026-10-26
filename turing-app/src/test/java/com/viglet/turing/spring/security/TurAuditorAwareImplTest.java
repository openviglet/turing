package com.viglet.turing.spring.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.viglet.turing.persistence.model.auth.TurUser;

class TurAuditorAwareImplTest {

    private final TurAuditorAwareImpl auditorAware = new TurAuditorAwareImpl();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static void signIn(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @Test
    void shouldReturnAdminWhenAuthenticationIsNull() {
        assertEquals(Optional.of("admin"), auditorAware.getCurrentAuditor());
    }

    @Test
    void shouldReturnLowercasePreferredUsernameForAnOAuth2User() {
        OAuth2User oauth2User = org.mockito.Mockito.mock(OAuth2User.class);
        when(oauth2User.getAttribute(TurAuditorAwareImpl.PREFERRED_USERNAME)).thenReturn("John.DOE");
        signIn(oauth2User);

        assertEquals(Optional.of("john.doe"), auditorAware.getCurrentAuditor());
    }

    @Test
    void shouldReturnLowercaseUsernameForASession() {
        TurUser turUser = TurUser.builder().username("ADMIN.User").build();
        signIn(new TurCustomUserDetails(turUser, List.of("ROLE_USER")));

        assertEquals(Optional.of("admin.user"), auditorAware.getCurrentAuditor());
    }

    /** T1126 — a Key request carries the key owner's TurUser, which was cast and threw. */
    @Test
    void shouldReturnTheKeyOwnerForAnApiKeyRequest() {
        signIn(TurUser.builder().username("Connector").build());

        assertEquals(Optional.of("connector"), auditorAware.getCurrentAuditor());
    }

    @Test
    void shouldFallBackToAdminForAnAnonymousRequest() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken("key",
                "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertEquals(Optional.of("admin"), auditorAware.getCurrentAuditor());
    }
}
