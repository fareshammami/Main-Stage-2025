package cqrses.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomKeycloakConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String REALM_ROLES_CLAIM_NAME = "realm_access";
    private static final String ROLES_CLAIM_NAME = "roles";
    private static final String CLIENT_ROLES_CLAIM_NAME = "resource_access";
    private static final String GROUPS_CLAIM_NAME = "groups";
    private static final String ROLES_PREFIX = "ROLE_";
    private static final String GROUPS_PREFIX = "GROUP_";

    /**
     * This method is responsible for converting the Jwt into an AbstractAuthenticationToken
     *
     * @param jwt the source object to convert, which must be an instance of {@code S} (never {@code null})
     * @return AbstractAuthenticationToken
     */
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt.getClaims());
        return new JwtAuthenticationToken(jwt, authorities);
    }

    /**
     * This method calls the clients,realm and groups authorities extraction methods
     *
     * @param claims List of claims from the token
     * @return Collection<GrantedAuthority>
     */
    private Collection<GrantedAuthority> extractAuthorities(Map<String, Object> claims) {
        if (!claims.containsKey(REALM_ROLES_CLAIM_NAME) && !claims.containsKey(CLIENT_ROLES_CLAIM_NAME) && !claims.containsKey(GROUPS_CLAIM_NAME)) {
            return Collections.emptyList();
        }
        Collection<GrantedAuthority> realmAuthorities = extractRealmAuthorities(claims);
        Collection<GrantedAuthority> clientAuthorities = extractClientAuthorities(claims);
        Collection<GrantedAuthority> groups = extractGroups(claims);
        realmAuthorities.addAll(clientAuthorities);
        realmAuthorities.addAll(groups);
        return realmAuthorities;
    }

    /**
     * This method extracts realm roles from the claims
     *
     * @param claims List of claims from the token
     * @return Collection<GrantedAuthority>
     */
    private Collection<GrantedAuthority> extractRealmAuthorities(Map<String, Object> claims) {
        if (!claims.containsKey(REALM_ROLES_CLAIM_NAME)) {
            return Collections.emptyList();
        }
        Map<String, Object> realmAccess = (Map<String, Object>) claims.get(REALM_ROLES_CLAIM_NAME);
        if (!realmAccess.containsKey(ROLES_CLAIM_NAME)) {
            return Collections.emptyList();
        }
        return ((Collection<String>) realmAccess.get(ROLES_CLAIM_NAME)).stream()
                .map(role -> ROLES_PREFIX + role) // Prefixing roles with "ROLE_" is a convention in Spring Security
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * This method extracts client roles from the claims
     *
     * @param claims List of claims from the token
     * @return Collection<GrantedAuthority>
     */
    private Collection<GrantedAuthority> extractClientAuthorities(Map<String, Object> claims) {
        Collection<GrantedAuthority> clientAuthorities = new ArrayList<>();
        if (!claims.containsKey(CLIENT_ROLES_CLAIM_NAME)) {
            return Collections.emptyList();
        }
        Map<String, Object> resourceAccess = (Map<String, Object>) claims.get(CLIENT_ROLES_CLAIM_NAME);
        for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
            Map<String, Object> clientAccess = (Map<String, Object>) entry.getValue();
            if (clientAccess.containsKey(ROLES_CLAIM_NAME)) {
                Collection<String> clientRoles = (Collection<String>) clientAccess.get(ROLES_CLAIM_NAME);
                clientAuthorities.addAll(clientRoles.stream()
                        .map(role -> ROLES_PREFIX + role)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList()));
            }
        }
        return clientAuthorities;
    }

    /**
     * Extracts a collection of granted authorities from the provided claims map.
     *
     * <p>This method checks for the presence of the groups claim in the token's claims.
     * If present, it transforms the group names into {@link GrantedAuthority} objects
     * with the appropriate prefix. If the groups claim is not found, an empty collection
     * is returned.</p>
     *
     * @param claims a map containing claims from the token, expected to include
     *               a key for the groups claim
     * @return a collection of {@link GrantedAuthority} objects representing the
     *         groups extracted from the claims, or an empty collection if no groups
     *         are found.
     */
    public Collection<GrantedAuthority> extractGroups(Map<String, Object> claims) {
        if (!claims.containsKey(GROUPS_CLAIM_NAME)) {
            return Collections.emptyList();
        }
        return ((Collection<String>) claims.get(GROUPS_CLAIM_NAME)).stream()
                .map(role -> GROUPS_PREFIX + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}