package cqrses.aop;

import cqrses.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserSyncAspect {

    private final Keycloak keycloakAdmin;
    private final UserService userService;

    @Before("@annotation(SyncUserFromKeycloak)")
    public void syncUserFromKeycloak() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof Jwt jwt) {
            String userId = jwt.getSubject();

            // 1. Get user info from Keycloak
            UserRepresentation keycloakUser = keycloakAdmin.realm("MainStageRealm")
                    .users().get(userId).toRepresentation();

            // 2. Sync into MongoDB
            userService.syncFromKeycloakUser(keycloakUser);
        }
    }
}
