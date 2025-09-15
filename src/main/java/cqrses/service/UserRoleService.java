package cqrses.service;

import cqrses.entity.Role;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.stereotype.Service;

import jakarta.ws.rs.ForbiddenException; // matches your stacktrace
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final Keycloak keycloakAdmin;       // from KeycloakAdminConfig
    private static final String REALM = "MainStageRealm";

    public void setRole(String userId, Role newRole) {
        try {
            UserResource userResource = keycloakAdmin.realm(REALM).users().get(userId);

            // 1) Remove existing 'admin' / 'user' realm roles if present (so we have single-role behavior)
            List<RoleRepresentation> assigned = userResource.roles().realmLevel().listAll();
            List<RoleRepresentation> toRemove = assigned.stream()
                    .filter(r -> r.getName().equalsIgnoreCase("admin") || r.getName().equalsIgnoreCase("user"))
                    .collect(Collectors.toList());

            if (!toRemove.isEmpty()) {
                userResource.roles().realmLevel().remove(toRemove);
            }

            // 2) Get the RoleRepresentation for the new role (Keycloak role name assumed lower-case 'admin'/'user')
            String roleName = newRole == Role.ADMIN ? "admin" : "user";
            RoleRepresentation roleRep = keycloakAdmin.realm(REALM).roles().get(roleName).toRepresentation();

            // 3) Add the new role
            userResource.roles().realmLevel().add(Collections.singletonList(roleRep));

            // OPTIONAL: force logout so user must re-login and receives an updated token
            // userResource.logout();

        } catch (ForbiddenException fe) {
            throw new IllegalStateException("Keycloak service account doesn't have permission to manage users. Check service account roles and credentials.", fe);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to set role in Keycloak: " + e.getMessage(), e);
        }
    }

    public Role getRoleFromKeycloak(String userId) {
        List<RoleRepresentation> assigned = keycloakAdmin.realm(REALM)
                .users()
                .get(userId)
                .roles()
                .realmLevel()
                .listAll();

        boolean isAdmin = assigned.stream().anyMatch(r -> r.getName().equalsIgnoreCase("admin"));
        return isAdmin ? Role.ADMIN : Role.USER;
    }
}
