package com.viglet.turing.spring.security.auth;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.viglet.turing.persistence.model.auth.TurUser;
import com.viglet.turing.persistence.model.dev.token.TurDevToken;
import com.viglet.turing.persistence.repository.auth.TurUserRepository;
import com.viglet.turing.persistence.repository.dev.token.TurDevTokenRepository;
import com.viglet.turing.spring.security.TurAuthorityResolver;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TurAuthTokenHeaderFilter extends OncePerRequestFilter {
    public static final String KEY = "Key";

    private final TurUserRepository turUserRepository;
    private final TurDevTokenRepository turDevTokenRepository;
    private final TurAuthorityResolver turAuthorityResolver;

    public TurAuthTokenHeaderFilter(TurUserRepository turUserRepository,
            TurDevTokenRepository turDevTokenRepository,
            TurAuthorityResolver turAuthorityResolver) {
        this.turUserRepository = turUserRepository;
        this.turDevTokenRepository = turDevTokenRepository;
        this.turAuthorityResolver = turAuthorityResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain)
            throws ServletException, IOException {
        String appId = request.getHeader(KEY);
        // T1210 — a usable key authenticates the request even when a session already
        // did: the key is what this request presents, and a session another key opened
        // would otherwise answer for it, in that key's partition or in none.
        if (appId != null) {
            // T646 / §XXXVII.8 — reject disabled/expired tokens.
            turDevTokenRepository.findByToken(appId).filter(TurDevToken::isUsable).ifPresent(token -> {
                String createdBy = token.getCreatedBy();
                TurUser turUser = findCreator(createdBy);
                // T1245 — the key is its creator even with no local row (a Keycloak user
                // the console never mirrored): the creator's name still resolves its
                // tenant memberships and the bootstrap admin, and names the principal.
                String username = turUser != null ? turUser.getUsername() : createdBy;
                // T1061 — resolve through TurAuthorityResolver, whose transaction keeps
                // the lazy role → privileges collection loadable. Walking it here, with
                // no session open on a key-only request, threw LazyInitializationException.
                Set<GrantedAuthority> authorities = new HashSet<>();
                turAuthorityResolver.resolve(username, authorities);
                Object principal = turUser != null ? turUser : username;
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        principal, null, authorities);
                // T1133 — a key bound to a partition of a shared site carries it.
                TurApiKeyPartitionContext.bind(request, token);
                // T1210 — and so does the authentication the session keeps, or a
                // request replaying the session's cookie would leave the partition.
                authenticationToken.setDetails(TurApiKeyPartitionContext.of(request)
                        .<Object>map(partition -> new TurApiKeyAuthenticationDetails(request, partition))
                        .orElseGet(() -> new WebAuthenticationDetailsSource().buildDetails(request)));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            });
        }
        filterChain.doFilter(request, response);
    }

    /**
     * T1245 — the key's creator as a local user. {@code createdBy} is stamped
     * lower-cased by the auditor, while a Keycloak user's row keeps the case Keycloak
     * or {@code turing.keycloak-admin-id} spells, so an exact match misses it.
     */
    private TurUser findCreator(String createdBy) {
        if (createdBy == null) {
            return null;
        }
        TurUser exact = turUserRepository.findByUsername(createdBy);
        return exact != null ? exact : turUserRepository.findFirstByUsernameIgnoreCase(createdBy);
    }
}
